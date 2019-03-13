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

import cats.MonadError
import cats.implicits._
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{ConfidenceLevel, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.controllers.{Accounts, DeviceVersion}

abstract class PreflightServiceImpl[F[_]](genericConnector: GenericConnector[F], minimumConfidenceLevel: Int)(
  // Need a MonadError because the `getVersion` code needs to recover exceptions
  implicit F: MonadError[F, Throwable]
) extends PreflightService[F] {

  // The authentication and auditing calls from the platform are based on Future so declare a couple of
  // methods that adapt away from Future to F that the live implementation can define.
  def retrieveAccounts(implicit hc: HeaderCarrier): F[(Option[Nino], Option[SaUtr], Credentials, ConfidenceLevel)]
  def auditing[T](service:          String, details: Map[String, String])(f: => F[T])(implicit hc: HeaderCarrier): F[T]

  def preFlight(deviceVersion: DeviceVersion, journeyId: Option[String])(implicit hc: HeaderCarrier): F[PreFlightCheckResponse] =
    auditing("preFlightCheck", Map.empty) {
      // `mapN` is not inherently parallel. I.e. the two functions won't get run concurrently. However,
      // the tuple will evaluate the two functions eagerly. Knowing that the concrete implementation
      // will use `Future`s means we know that the live system will, in practice, give us concurrency
      // of the two calls. Because of this it's not worth the extra complexity of using `parMapN`, even
      // that that would give a more correct description of what's going on.
      (getAccounts(journeyId), getVersion(deviceVersion, journeyId)).mapN { (accounts, versionUpdate) =>
        PreFlightCheckResponse(versionUpdate, accounts.copy())
      }
    }

  private def getAccounts(journeyId: Option[String])(implicit hc: HeaderCarrier): F[Accounts] =
    retrieveAccounts.map {
      case (nino, saUtr, credentials, confidenceLevel) =>
        if (credentials.providerType != "GovernmentGateway") throw new UnsupportedAuthProvider
        Accounts(
          nino,
          saUtr,
          minimumConfidenceLevel > confidenceLevel.level,
          journeyId.filter(id => id.length > 0).getOrElse(randomUUID().toString)
        )
    }

  private def getVersion(deviceVersion: DeviceVersion, journeyId: Option[String])(implicit hc: HeaderCarrier): F[Boolean] = {
    val journeyIdParam = journeyId.fold("")(id => s"?journeyId=$id")
    val path           = s"/mobile-version-check$journeyIdParam"

    if (deviceVersion.os.toLowerCase.contains("windows")) F.pure(true)
    else {
      genericConnector
        .doPost[JsValue](toJson(deviceVersion), "mobile-version-check", path, hc)
        .map(resp => (resp \ "upgradeRequired").as[Boolean])
        .recover {
          // Default to false - i.e. no upgrade required.
          case exception: Exception =>
            Logger.warn(s"Native Error - failure with processing version check. Exception is $exception")
            false
        }
    }
  }
}
