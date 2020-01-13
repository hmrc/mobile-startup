/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

case class PreFlightCheckResponse(nino: Option[Nino], saUtr: Option[SaUtr], routeToIV: Boolean)

object PreFlightCheckResponse {

  implicit val writes: Writes[PreFlightCheckResponse] = new Writes[PreFlightCheckResponse] {
    def withNino(nino: Option[Nino]): JsObject = nino.fold(Json.obj()) { found => Json.obj("nino" -> found.value)
    }

    def withSaUtr(saUtr: Option[SaUtr]): JsObject = saUtr.fold(Json.obj()) { found => Json.obj("saUtr" -> found.value)
    }

    def writes(preFlightCheckResponse: PreFlightCheckResponse): JsObject =
      withNino(preFlightCheckResponse.nino) ++ withSaUtr(preFlightCheckResponse.saUtr) ++ Json
        .obj("routeToIV" -> preFlightCheckResponse.routeToIV)
  }

}

@ImplementedBy(classOf[LivePreFlightService])
trait PreFlightService[F[_]] {
  def preFlight(journeyId: String)(implicit hc: HeaderCarrier): F[PreFlightCheckResponse]
}
