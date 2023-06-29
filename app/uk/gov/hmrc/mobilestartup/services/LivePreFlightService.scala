/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel, Enrolments}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.{Activated, CidPerson, EnrolmentStoreResponse, NoEnrolment, NoUtr, NotYetActivated, WrongAccount}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.service.Auditor
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND}

import scala.concurrent.{ExecutionContext, Future}

class LivePreFlightService @Inject() (
  genericConnector:                                                              GenericConnector[Future],
  val auditConnector:                                                            AuditConnector,
  val authConnector:                                                             AuthConnector,
  @Named("appName") val appName:                                                 String,
  @Named("controllers.confidenceLevel") val confLevel:                           Int,
  @Named("feature.annualTaxSummaryLink") val showATSLink:                        Boolean,
  @Named("enableMultipleGGIDCheck.ios") val multipleGGIDCheckEnabledIos:         Boolean,
  @Named("enableMultipleGGIDCheck.android") val multipleGGIDCheckEnabledAndroid: Boolean
)(implicit executionContext:                                                     ExecutionContext)
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
    (Option[Nino], Option[SaUtr], Option[Credentials], ConfidenceLevel, Option[AnnualTaxSummaryLink], Enrolments)
  ] =
    authConnector
      .authorise(EmptyPredicate, nino and saUtr and credentials and confidenceLevel and allEnrolments)
      .map {
        case foundNino ~ foundSaUtr ~ creds ~ conf ~ foundEnrolments =>
          (foundNino.map(Nino(_)), foundSaUtr.map(SaUtr(_)), creds, conf, getATSLink(foundEnrolments), foundEnrolments)
      }

  override def getUtr(
    foundUtr:    Option[SaUtr],
    foundNino:   Option[Nino],
    enrolments:  Enrolments
  )(implicit hc: HeaderCarrier
  ): Future[Option[Utr]] = {
    val activatedSaUtr = getActivatedSaUtr(enrolments)
    if (activatedSaUtr.isDefined) Future successful activatedSaUtr.map(utr => Utr(Some(utr), Activated))
    else if (foundUtr.isDefined) {
      Future successful foundUtr.flatMap(utr => Some(Utr(Some(SaUtr(utr.utr)), NotYetActivated)))
    } else {
      foundNino
        .map { nino =>
          for {
            saUtrOnCid <- getUtrFromCID(nino.nino).recover {
                           case e: NotFoundException => Some(SaUtr("NOT_FOUND"))
                         }
            hasPrincipalIds <- doesUtrHavePrincipalIds(saUtrOnCid)
          } yield {
            saUtrOnCid match {
              case Some(utr) =>
                if (utr.utr == "NOT_FOUND") Some(Utr.noUtr)
                else
                  hasPrincipalIds match {
                    case None       => None
                    case Some(true) => Some(Utr(Some(SaUtr(utr.value)), WrongAccount))
                    case _          => Some(Utr(Some(SaUtr(utr.value)), NoEnrolment))
                  }
              case _ => None
            }
          }
        }
        .getOrElse(Future successful Some(Utr.noUtr))
    }
  }

  override def doesUserHaveMultipleGGIDs(enrolments: Enrolments)(implicit hc: HeaderCarrier): Boolean = {
    val userAgentHeader            = hc.otherHeaders.toMap.getOrElse("User-Agent", "No User-Agent").toLowerCase
    val userMissingHmrcPtEnrolment = !enrolments.enrolments.exists(_.key == "HMRC-PT")
    if (userAgentHeader.contains("ios")) {
      logger.info("iOS user missing HMRC-PT enrolment")
      if (multipleGGIDCheckEnabledIos) userMissingHmrcPtEnrolment else false
    } else if (userAgentHeader.contains("android")) {
      logger.info("Android user missing HMRC-PT enrolment")
      if (multipleGGIDCheckEnabledAndroid) userMissingHmrcPtEnrolment else false
    } else {
      logger.info(s"User-Agent not recognised or missing: $userAgentHeader")
      false
    }
  }

  private def getATSLink(enrolments: Enrolments): Option[AnnualTaxSummaryLink] =
    if (showATSLink) {
      if (getActivatedSaUtr(enrolments).isDefined) Some(AnnualTaxSummaryLink("/annual-tax-summary", "SA"))
      else Some(AnnualTaxSummaryLink("/annual-tax-summary/paye/main", "PAYE"))
    } else None

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
          throw new NotFoundException("No UTR found on CID")
        case e: NotFoundException =>
          logger.info(s"Call to CID failed - No record for the Nino: $nino found on CID.")
          throw e
        case e: UpstreamErrorResponse =>
          logger.info(s"Call to CID failed $e")
          None
        case _ =>
          logger.info(s"Call to CID failed")
          None
      }
    cidPerson.map(_.map(_.ids.saUtr.isDefined)).flatMap {
      case Some(true)  => cidPerson.map(_.flatMap(_.ids.saUtr))
      case Some(false) => throw new NotFoundException("No UTR found on CID")
      case None        => Future successful None
    }
  }

  private def doesUtrHavePrincipalIds(utr: Option[SaUtr])(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    if (utr.map(_.value).contains("NOT_FOUND")) Future successful None
    else {
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

}
