/*
 * Copyright 2021 HM Revenue & Customs
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
import cats.implicits._
import eu.timepit.refined.auto._
import play.api.Logger

import javax.inject.{Inject, Named}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ItmpName, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel, Enrolments}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse, UpstreamErrorResponse}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.{CidPerson, EnrolmentStoreResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.service.Auditor
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND}

import scala.concurrent.{ExecutionContext, Future}

class LivePreFlightService @Inject() (
  genericConnector:                                       GenericConnector[Future],
  val auditConnector:                                     AuditConnector,
  val authConnector:                                      AuthConnector,
  @Named("appName") val appName:                          String,
  @Named("controllers.confidenceLevel") val confLevel:    Int,
  @Named("feature.annualTaxSummaryLink") val showATSLink: Boolean
)(implicit executionContext:                              ExecutionContext)
    extends PreFlightServiceImpl[Future](genericConnector, confLevel)
    with AuthorisedFunctions
    with Auditor {

  val logger: Logger = Logger(this.getClass)

  // Just adapting from `F` to `Future` here
  override def auditing[T](
    service:     String,
    details:     Map[String, String]
  )(f:           => Future[T]
  )(implicit hc: HeaderCarrier
  ): Future[T] =
    withAudit(service, details)(f)

  // The retrieval function is really hard to dummy out in tests because of it's polymorphic nature, and the `~` trick doesn't
  // help, but isolating it here and adapting to the concrete tuple of results we are expecting makes testing of the logic in
  // `PreFlightServiceImpl` much easier.
  override def retrieveAccounts(implicit hc: HeaderCarrier): Future[
    (Option[Nino],
     Option[SaUtr],
     Option[Credentials],
     ConfidenceLevel,
     Option[ItmpName],
     Option[AnnualTaxSummaryLink],
     Enrolments)
  ] =
    authConnector
      .authorise(EmptyPredicate,
                 nino and saUtr and credentials and confidenceLevel and itmpName and allEnrolments and name)
      .map {
        case foundNino ~ foundSaUtr ~ creds ~ conf ~ Some(itmpName) ~ foundEnrolments ~ _ =>
          (foundNino.map(Nino(_)),
           foundSaUtr.map(SaUtr(_)),
           creds,
           conf,
           Some(itmpName),
           getATSLink(foundEnrolments),
           foundEnrolments)
        case foundNino ~ foundSaUtr ~ creds ~ conf ~ None ~ foundEnrolments ~ Some(name) =>
          (foundNino.map(Nino(_)),
           foundSaUtr.map(SaUtr(_)),
           creds,
           conf,
           Some(ItmpName(givenName = name.name, None, familyName = name.lastName)),
           getATSLink(foundEnrolments),
           foundEnrolments)
        case foundNino ~ foundSaUtr ~ creds ~ conf ~ itmpName ~ foundEnrolments ~ _ =>
          (foundNino.map(Nino(_)),
           foundSaUtr.map(SaUtr(_)),
           creds,
           conf,
           itmpName,
           getATSLink(foundEnrolments),
           foundEnrolments)

      }

  private def getATSLink(enrolments: Enrolments): Option[AnnualTaxSummaryLink] =
    if (showATSLink) {
      if (getActivatedSaUtr(enrolments).isDefined) Some(AnnualTaxSummaryLink("/annual-tax-summary", "SA"))
      else Some(AnnualTaxSummaryLink("/annual-tax-summary/paye/main", "PAYE"))
    } else None

  override def getUtr(
    foundUtr:    Option[SaUtr],
    foundNino:   Option[Nino],
    enrolments:  Enrolments
  )(implicit hc: HeaderCarrier
  ): Future[Option[Utr]] = {
    val activatedSaUtr = getActivatedSaUtr(enrolments)
    if (activatedSaUtr.isDefined) Future successful activatedSaUtr.map(utr => Utr(utr, None))
    else if (foundUtr.isDefined) {
      Future successful foundUtr.flatMap(utr =>
        Some(
          Utr(SaUtr(utr.utr),
              Some("/enrolment-management-frontend/IR-SA/get-access-tax-scheme?continue=/personal-account"))
        )
      )
    } else {
      foundNino
        .map { nino =>
          for {
            saUtrOnCid      <- getUtrFromCID(nino.nino)
            hasPrincipalIds <- doesUtrHavePrincipalIds(saUtrOnCid)
          } yield {
            saUtrOnCid.flatMap { utr =>
              hasPrincipalIds match {
                case None => None
                case Some(true) =>
                  Some(
                    Utr(SaUtr(utr.value), Some("/personal-account/self-assessment/signed-in-wrong-account"))
                  )
                case _ =>
                  Some(
                    Utr(SaUtr(utr.value), Some("/business-account/add-tax/self-assessment/try-iv?origin=pta-sa"))
                  )
              }
            }
          }
        }
        .getOrElse(Future successful None)
    }
  }

  private def getActivatedSaUtr(enrolments: Enrolments): Option[SaUtr] =
    enrolments.enrolments
      .find(_.key == "IR-SA")
      .flatMap { enrolment =>
        enrolment.identifiers
          .find(id => id.key == "UTR" && enrolment.state == "Activated")
          .map(key => SaUtr(key.value))
      }

  private def getUtrFromCID(nino: String)(implicit hc: HeaderCarrier): Future[Option[SaUtr]] = {
    val cidPerson: Future[Option[CidPerson]] = genericConnector
      .cidGet("citizen-details", s"/citizen-details/nino/$nino", hc)
      .map(p => Some(p))
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == BAD_REQUEST =>
          logger.info(s"Call to CID failed - Nino is invalid: $nino.")
          None
        case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND =>
          logger.info(s"Call to CID failed - No record for the Nino: $nino found on CID.")
          None
        case e: UpstreamErrorResponse =>
          logger.info(s"Call to CID failed $e")
          None
        case _ =>
          logger.info(s"Call to CID failed")
          None
      }
    cidPerson.map(_.flatMap(_.ids.saUtr))
  }

  private def doesUtrHavePrincipalIds(utr: Option[SaUtr])(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    utr
      .map { utr =>
        val enrolments: Future[Option[EnrolmentStoreResponse]] = genericConnector
          .enrolmentStoreGet(
            "enrolment-store-proxy",
            s"/enrolment-store-proxy/enrolment-store/enrolments/IR-SA~UTR~${utr.utr}/users?type=principal",
            hc
          )
          .map(Some(_))
          .recover {
            case e: UpstreamErrorResponse =>
              logger.info(s"Call to Enrolment Store failed $e")
              None
            case _ =>
              logger.info(s"Call to Enrolment Store failed")
              None
          }
        enrolments.map(_.map(_.principalUserIds.nonEmpty))
      }
      .getOrElse(Future successful None)
}
