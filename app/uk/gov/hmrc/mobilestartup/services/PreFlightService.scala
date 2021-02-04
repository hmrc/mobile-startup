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
import com.google.inject.ImplementedBy
import play.api.libs.json.{Format, JsObject, Json, Writes}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.{JourneyId, LinkDestination}
import uk.gov.hmrc.mobilestartup.model.types._

case class PreFlightCheckResponse(
  nino:                 Option[Nino],
  saUtr:                Option[SaUtr],
  routeToIV:            Boolean,
  name:                 Option[Name],
  annualTaxSummaryLink: Option[AnnualTaxSummaryLink] = None)

object PreFlightCheckResponse {

  implicit val writes: Writes[PreFlightCheckResponse] = new Writes[PreFlightCheckResponse] {

    def withNino(nino: Option[Nino]): JsObject = nino.fold(Json.obj()) { found =>
      Json.obj("nino" -> found.value)
    }

    def withSaUtr(saUtr: Option[SaUtr]): JsObject = saUtr.fold(Json.obj()) { found =>
      Json.obj("saUtr" -> found.value)
    }

    def withName(fullName: Option[Name]): JsObject = fullName.fold(Json.obj()) { found =>
      Json.obj("name" -> (found.name.getOrElse("") + " " + found.lastName.getOrElse("")).trim)
    }

    def withATSLink(atsLink: Option[AnnualTaxSummaryLink]): JsObject = atsLink.fold(Json.obj()) { found =>
      Json.obj("annualTaxSummaryLink" -> found)
    }

    def writes(preFlightCheckResponse: PreFlightCheckResponse): JsObject =
      withNino(preFlightCheckResponse.nino) ++ withSaUtr(preFlightCheckResponse.saUtr) ++ Json
        .obj("routeToIV" -> preFlightCheckResponse.routeToIV) ++ withName(preFlightCheckResponse.name) ++
      withATSLink(preFlightCheckResponse.annualTaxSummaryLink)
  }

}

case class AnnualTaxSummaryLink(
  link:        String,
  destination: LinkDestination)

object AnnualTaxSummaryLink { implicit val formats: Format[AnnualTaxSummaryLink] = Json.format[AnnualTaxSummaryLink] }

@ImplementedBy(classOf[LivePreFlightService])
trait PreFlightService[F[_]] {
  def preFlight(journeyId: JourneyId)(implicit hc: HeaderCarrier): F[PreFlightCheckResponse]
}
