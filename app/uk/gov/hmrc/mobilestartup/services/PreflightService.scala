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
import com.google.inject.ImplementedBy
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.controllers.{Accounts, DeviceVersion}

case class PreFlightCheckResponse(upgradeRequired: Boolean, accounts: Accounts)

object PreFlightCheckResponse {

  implicit val accountsFmt: Writes[Accounts] = Accounts.writes

  implicit val preFlightCheckResponseFmt: Writes[PreFlightCheckResponse] = Json.writes[PreFlightCheckResponse]
}

@ImplementedBy(classOf[LivePreflightService])
trait PreflightService[F[_]] {
  def preFlight(request: DeviceVersion, journeyId: Option[String])(implicit hc: HeaderCarrier): F[PreFlightCheckResponse]
}