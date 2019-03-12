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

import cats.implicits._
import javax.inject.{Inject, Named}
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.controllers.{Accounts, DeviceVersion}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.service.Auditor

import scala.concurrent.{ExecutionContext, Future}

case class PreFlightCheckResponse(upgradeRequired: Boolean, accounts: Accounts)

object PreFlightCheckResponse {

  implicit val accountsFmt: Format[Accounts] = Accounts.formats

  implicit val preFlightCheckResponseFmt: OFormat[PreFlightCheckResponse] = Json.format[PreFlightCheckResponse]
}

class LivePreflightService @Inject()(
  genericConnector:                                    GenericConnector[Future],
  val auditConnector:                                  AuditConnector,
  val authConnector:                                   AuthConnector,
  @Named("appName") val appName:                       String,
  @Named("controllers.confidenceLevel") val confLevel: Int
)(
  implicit executionContext: ExecutionContext
) extends PreflightService
    with AuthorisedFunctions
    with Auditor {

  def preFlight(request: DeviceVersion, journeyId: Option[String])(implicit hc: HeaderCarrier): Future[PreFlightCheckResponse] = {
    audit("preFlightCheck", Map.empty)
    (getAccounts(journeyId), getVersion(request, journeyId)).mapN { (accounts, versionUpdate) =>
      PreFlightCheckResponse(versionUpdate, accounts.copy())
    }
  }

  def getAccounts(journeyId: Option[String])(implicit hc: HeaderCarrier): Future[Accounts] =
    authorised()
      .retrieve(nino and saUtr and affinityGroup and credentials and confidenceLevel) {
        case foundNino ~ foundSaUtr ~ foundAffinityGroup ~ foundCredentials ~ foundConfidenceLevel =>
          val routeToIV         = confLevel > foundConfidenceLevel.level
          val journeyIdentifier = journeyId.filter(id => id.length > 0).getOrElse(randomUUID().toString)
          if (foundCredentials.providerType != "GovernmentGateway") throw new UnsupportedAuthProvider
          Future.successful(
            Accounts(
              foundNino.map(Nino(_)),
              foundSaUtr.map(SaUtr(_)),
              routeToIV,
              journeyIdentifier,
              foundCredentials.providerId,
              foundAffinityGroup.get.toString))
      }

  def getVersion(request: DeviceVersion, journeyId: Option[String])(implicit hc: HeaderCarrier): Future[Boolean] = {
    def buildJourney: String = journeyId.fold("")(id => s"?journeyId=$id")

    val path = s"/mobile-version-check$buildJourney"

    if (request.os.toLowerCase.contains("windows")) Future.successful(true)
    else {
      genericConnector
        .doPost[JsValue](toJson(request), "mobile-version-check", path, hc)
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
