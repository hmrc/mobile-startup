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
import cats.implicits._
import play.api.Logger
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import eu.timepit.refined.auto._
import uk.gov.hmrc.mobilestartup.model.Activated

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
     Option[String])
  ]

  def getUtr(
    foundUtr:    Option[SaUtr],
    foundNino:   Option[Nino],
    enrolments:  Enrolments
  )(implicit hc: HeaderCarrier
  ): F[
    (Option[Utr])
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

    val accountsRetrieved: F[PreFlightCheckResponse] = retrieveAccounts.map {
      case (nino, saUtr, credentials, confidenceLevel, annualTaxSummaryLink, enrolments, internalId) =>
        println("INTERNAL ID = " + internalId.getOrElse("NO ID FOUND"))
        if (credentials.getOrElse(Credentials("Unsupported", "Unsupported")).providerType != "GovernmentGateway")
          throw new UnsupportedAuthProvider
        PreFlightCheckResponse(nino,
                               saUtr,
                               minimumConfidenceLevel > confidenceLevel.level,
                               annualTaxSummaryLink,
                               None,
                               enrolments,
                               demoAccount = checkForDemoAccountId(internalId))
    }
    for {
      account    <- accountsRetrieved
      utrDetails <- getUtr(account.saUtr, account.nino, account.enrolments)
    } yield {
      if (account.demoAccount) {
        logger.info("Demo account Internal ID found, returning Sandbox data")
        PreFlightCheckResponse(
          Some(Nino("CS700100A")),
          None,
          routeToIV = false,
          Some(AnnualTaxSummaryLink("/", "PAYE")),
          Some(Utr(saUtr = Some(SaUtr("1234567890")), status = Activated)),
          Enrolments(Set.empty),
          demoAccount = true
        )
      } else
        PreFlightCheckResponse(
          account.nino,
          account.saUtr,
          account.routeToIV,
          account.annualTaxSummaryLink,
          utrDetails,
          account.enrolments,
          doesUserHaveMultipleGGIDs(account.enrolments, account.nino)
        )
    }
  }

  private def checkForDemoAccountId(internalId: Option[String]): Boolean = {
    val accountId = internalId.getOrElse("")
    (accountId == storeReviewAccountInternalId || accountId == appTeamAccountInternalId)
  }

}
