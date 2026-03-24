/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobilestartup.services
import cats.MonadError
import cats.implicits.*
import play.api.Logger
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolments, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.types.JourneyId
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.fromStringtoLinkDestination
import uk.gov.hmrc.mobilestartup.model.{Activated, PertaxResponse, RetrieveAccountsResponse}

import scala.concurrent.ExecutionContext

abstract class PreFlightServiceImpl[F[_]](
  genericConnector:             GenericConnector[F],
  minimumConfidenceLevel:       Int,
  storeReviewAccountInternalId: String,
  appTeamAccountInternalId:     String
)(implicit F:                   MonadError[F, Throwable])
    extends PreFlightService[F] {

  val logger: Logger = Logger(this.getClass)

  // The authentication and auditing calls from the platform are based on Future so declare a couple of
  // methods that adapt away from Future to F that the live implementation can define.
  def retrieveAccounts(implicit hc: HeaderCarrier): F[
    (Option[Nino],
     Option[SaUtr],
     Option[Credentials],
     ConfidenceLevel,
     Option[AnnualTaxSummaryLink],
     Enrolments,
     Option[String],
     Option[AffinityGroup])
  ]

  def getUtr(
    foundUtr:    Option[SaUtr],
    foundNino:   Option[Nino],
    enrolments:  Enrolments
  )(implicit hc: HeaderCarrier
  ): F[
    Option[Utr]
  ]

  def auditing[T](
    service:     String,
    details:     Map[String, String]
  )(f:           => F[T]
  )(implicit hc: HeaderCarrier
  ): F[T]

  def preFlight(
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): F[PreFlightCheckResponse] =
    auditing("preFlightCheck", Map.empty) {
      getPreFlightCheckResponse(journeyId)
    }

  def doesUserHaveMultipleGGIDs(
    enrolments:  Enrolments,
    nino:        Option[Nino]
  )(implicit hc: HeaderCarrier
  ): Boolean

  private def getPreFlightCheckResponse(journeyId: JourneyId)(implicit hc: HeaderCarrier): F[PreFlightCheckResponse] = {

    val accountsRetrieved: F[RetrieveAccountsResponse] = retrieveAccounts.map {
      case (nino, saUtr, credentials, confidenceLevel, annualTaxSummaryLink, enrolments, internalId, affinityGroup) =>
        if (credentials.getOrElse(Credentials("Unsupported", "Unsupported")).providerType != "GovernmentGateway")
          throw new UnsupportedAuthProvider
        RetrieveAccountsResponse(nino,
                                 saUtr,
                                 credentials,
                                 confidenceLevel,
                                 annualTaxSummaryLink,
                                 enrolments,
                                 internalId,
                                 affinityGroup)
    }
    for {
      accountDetails <- accountsRetrieved
      utrDetails     <- getUtr(accountDetails.saUtr, accountDetails.nino, accountDetails.enrolments)
      pertaxResponse <- if (!checkForDemoAccountId(accountDetails.internalId)) {
        genericConnector.doPost[PertaxResponse](serviceName = "pertax", path = "/pertax/authorise", hc = hc)
      } else PertaxResponse("ACCESS_GRANTED","access for demo account").pure[F]
    } yield {
      if (checkForDemoAccountId(accountDetails.internalId)) {
        logger.info("Demo account Internal ID found, returning Sandbox data")
        PreFlightCheckResponse(
          Some(Nino("CS700100A")),
          routeToIV = false,
          Some(AnnualTaxSummaryLink("/", fromStringtoLinkDestination("PAYE"))),
          Some(Utr(saUtr = Some(SaUtr("1234567890")), status = Activated)),
          Enrolments(Set.empty),
          demoAccount = true,
          isEligible = true
        )
      } else {
         (accountDetails.affinityGroup, pertaxResponse) match
          case (Some(Agent), _) =>
            logger.info("Agent account is being used to login")
            PreFlightCheckResponse(
              accountDetails.nino,
              false,
              accountDetails.annualTaxSummaryLink,
              utrDetails,
              accountDetails.enrolments,
              doesUserHaveMultipleGGIDs(accountDetails.enrolments, accountDetails.nino),
              isEligible = false,
              blockReason = Some("Agents not allowed")
            )

          case (_, PertaxResponse("MCI_RECORD", _)) =>
            logger.info("Individual has an MCI Record")
            PreFlightCheckResponse(
            nino = accountDetails.nino,
            routeToIV = false,
            annualTaxSummaryLink = None,
            utr = None,
            enrolments = accountDetails.enrolments,
            isEligible = false,
            blockReason = Some("Manual correspondence indicator is set")
          )
          case (_, PertaxResponse("DECEASED_RECORD", _)) =>
            logger.info("Individual is a deceased")
            PreFlightCheckResponse(
            nino = accountDetails.nino,
            routeToIV = false,
            annualTaxSummaryLink = None,
            utr = None,
            enrolments = accountDetails.enrolments,
            isEligible = false,
            blockReason = Some("User is deceased")
          )
          case (_, PertaxResponse("DESIGNATORY_DETAILS_NOT_FOUND", _)) =>
            logger.info("Individual account missed adult registration")
            PreFlightCheckResponse(
            nino = accountDetails.nino,
            routeToIV = false,
            annualTaxSummaryLink = None,
            utr = None,
            enrolments = accountDetails.enrolments,
            isEligible = false,
            blockReason = Some("Juvenile record missed adult registration")
          )
          case (Some(Organisation), _) =>
            logger.info("Organisation account is being used to login")
            PreFlightCheckResponse(
              accountDetails.nino,
              if (hasPTEnrolement(accountDetails.enrolments)) {
                minimumConfidenceLevel > accountDetails.confLevel.level
              }
              else false,
              accountDetails.annualTaxSummaryLink,
              utrDetails,
              accountDetails.enrolments,
              doesUserHaveMultipleGGIDs(accountDetails.enrolments, accountDetails.nino),
              isEligible = hasPTEnrolement(accountDetails.enrolments),
              blockReason = if (!hasPTEnrolement(accountDetails.enrolments)) {
                Some("Org not authorised")
              }
              else None
            )
          case (_, PertaxResponse("ACCESS_GRANTED", _)) =>
              logger.info("Individual account is being used to login")
              PreFlightCheckResponse(
                accountDetails.nino,
                minimumConfidenceLevel > accountDetails.confLevel.level,
                accountDetails.annualTaxSummaryLink,
                utrDetails,
                accountDetails.enrolments,
                doesUserHaveMultipleGGIDs(accountDetails.enrolments, accountDetails.nino),
                isEligible = true
              )
          case _ => logger.info("Error in Pre-flight check")
            throw InternalServerException(s"Pre-flight call failed with exception")

      }

    }
  }

  private def checkForDemoAccountId(internalId: Option[String]): Boolean = {
    val accountId = internalId.getOrElse("")
    accountId == storeReviewAccountInternalId || accountId == appTeamAccountInternalId
  }

  private def hasPTEnrolement(enrolments: Enrolments): Boolean = {
    val presentPTEnrolment = getKeyIdentifierAndState(enrolments, "HMRC-PT")
    presentPTEnrolment match
      case Some("HMRC-PT", "Activated") => true
      case _ => false
  }

  private def getKeyIdentifierAndState(enrolments: Enrolments, key: String): Option[(String, String)] =
    enrolments.getEnrolment(key).map { enr =>
      (enr.key, enr.state)
    }

}
