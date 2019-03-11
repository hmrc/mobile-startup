/*
 * Copyright 2019 HM Revenue & Customs
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
import java.util.UUID.randomUUID

import javax.inject.{Inject, Named}
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.controllers.PreFlightRequest
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.service.Auditor

import scala.concurrent.{ExecutionContext, Future}

case class Accounts(
  nino:                         Option[Nino],
  saUtr:                        Option[SaUtr],
  routeToIV:                    Boolean,
  journeyId:                    String,
  credId:                       String,
  affinityGroup:                String,
  @Deprecated routeToTwoFactor: Boolean = false)

object Accounts {
  implicit val reads: Reads[Accounts] = (
    (JsPath \ "nino").readNullable[Nino] and
      (JsPath \ "saUtr").readNullable[SaUtr] and
      (JsPath \ "routeToIV").read[Boolean] and
      (JsPath \ "journeyId").read[String] and
      (JsPath \ "credId").read[String] and
      (JsPath \ "affinityGroup").read[String] and
      (JsPath \ "routeToTwoFactor").read[Boolean]
  )(Accounts.apply _)

  implicit val writes: Writes[Accounts] = new Writes[Accounts] {
    def withNino(nino: Option[Nino]): JsObject = nino.fold(Json.obj()) { found =>
      Json.obj("nino" -> found.value)
    }

    def withSaUtr(saUtr: Option[SaUtr]): JsObject = saUtr.fold(Json.obj()) { found =>
      Json.obj("saUtr" -> found.value)
    }

    def writes(accounts: Accounts): JsObject =
      withNino(accounts.nino) ++ withSaUtr(accounts.saUtr) ++ Json
        .obj("routeToIV" -> accounts.routeToIV, "routeToTwoFactor" -> accounts.routeToTwoFactor, "journeyId" -> accounts.journeyId)
  }

  implicit val formats: Format[Accounts] = Format(reads, writes)
}

case class DeviceVersion(os: String, version: String)

object DeviceVersion {
  implicit val formats: Format[DeviceVersion] = Json.format[DeviceVersion]
}

case class PreFlightCheckResponse(upgradeRequired: Boolean, accounts: Accounts)

object PreFlightCheckResponse {

  implicit val accountsFmt: Format[Accounts] = Accounts.formats

  implicit val preFlightCheckResponseFmt: OFormat[PreFlightCheckResponse] = Json.format[PreFlightCheckResponse]
}

class PreflightService @Inject()(
  genericConnector:                                    GenericConnector[Future],
  val auditConnector:                                  AuditConnector,
  val authConnector:                                   AuthConnector,
  @Named("appName") val appName:                       String,
  @Named("controllers.confidenceLevel") val confLevel: Int
)(
  implicit ec: ExecutionContext
) extends AuthorisedFunctions
    with Auditor {

  def preFlight(request: PreFlightRequest, journeyId: Option[String])(implicit hc: HeaderCarrier): Future[PreFlightCheckResponse] =
    for {
      accounts      <- getAccounts(journeyId)
      versionUpdate <- getVersion(request, journeyId)
    } yield {
      PreFlightCheckResponse(versionUpdate, accounts.copy())
    }

  def getAccounts(journeyId: Option[String])(implicit hc: HeaderCarrier): Future[Accounts] =
    authorised()
      .retrieve(nino and saUtr and affinityGroup and credentials and confidenceLevel) {
        case foundNino ~ foundSaUtr ~ foundAffinityGroup ~ foundCredentials ~ foundConfidenceLevel =>
          val routeToIV         = confLevel > foundConfidenceLevel.level
          val journeyIdentifier = journeyId.filter(id => id.length > 0).getOrElse(randomUUID().toString)
          if (foundCredentials.providerType != "GovernmentGateway") throw new UnsupportedAuthProvider
          Future(
            Accounts(
              foundNino.map(Nino(_)),
              foundSaUtr.map(SaUtr(_)),
              routeToIV,
              journeyIdentifier,
              foundCredentials.providerId,
              foundAffinityGroup.get.toString))
      }

  def getVersion(request: PreFlightRequest, journeyId: Option[String])(implicit hc: HeaderCarrier): Future[Boolean] = {
    def buildJourney: String = journeyId.fold("")(id => s"?journeyId=$id")

    val device = DeviceVersion(request.os, request.version)
    val path   = s"/mobile-version-check$buildJourney"

    if (request.os.toLowerCase.contains("windows")) Future successful true
    else {
      genericConnector
        .doPost[JsValue](toJson(device), "mobile-version-check", path, hc)
        .map { resp =>
          (resp \ "upgradeRequired").as[Boolean]
        }
        .recover {
          // Default to false - i.e. no upgrade required.
          case exception: Exception =>
            Logger.warn(s"Native Error - failure with processing version check. Exception is $exception")
            false
        }
    }
  }
}
