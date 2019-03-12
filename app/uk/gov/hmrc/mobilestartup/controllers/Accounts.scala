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

package uk.gov.hmrc.mobilestartup.controllers
import play.api.libs.json._
import uk.gov.hmrc.domain.{Nino, SaUtr}

case class Accounts(nino: Option[Nino], saUtr: Option[SaUtr], routeToIV: Boolean, journeyId: String)

object Accounts {
  implicit val writes: Writes[Accounts] = new Writes[Accounts] {
    def withNino(nino: Option[Nino]): JsObject = nino.fold(Json.obj()) { found =>
      Json.obj("nino" -> found.value)
    }

    def withSaUtr(saUtr: Option[SaUtr]): JsObject = saUtr.fold(Json.obj()) { found =>
      Json.obj("saUtr" -> found.value)
    }

    def writes(accounts: Accounts): JsObject =
      withNino(accounts.nino) ++ withSaUtr(accounts.saUtr) ++ Json
        .obj("routeToIV" -> accounts.routeToIV, "journeyId" -> accounts.journeyId)
  }

}
