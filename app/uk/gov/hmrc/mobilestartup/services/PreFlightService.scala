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
import play.api.libs.json
import play.api.libs.json.{Format, JsObject, Json, Writes}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.ItmpName
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.{JourneyId, LinkDestination}
import uk.gov.hmrc.mobilestartup.model.types._

import scala.concurrent.{ExecutionContext, Future}

case class PreFlightCheckResponse(
  nino:                 Option[Nino],
  saUtr:                Option[SaUtr],
  routeToIV:            Boolean,
  name:                 Option[ItmpName],
  annualTaxSummaryLink: Option[AnnualTaxSummaryLink] = None,
  utr:                  Option[Utr],
  enrolments:           Enrolments)

object PreFlightCheckResponse {

  implicit val writes: Writes[PreFlightCheckResponse] = new Writes[PreFlightCheckResponse] {

    def withNino(nino: Option[Nino]): JsObject = nino.fold(Json.obj()) { found =>
      Json.obj("nino" -> found.value)
    }

    def withSaUtr(saUtr: Option[SaUtr]): JsObject = saUtr.fold(Json.obj()) { found =>
      Json.obj("saUtr" -> found.value)
    }

    def withName(fullName: Option[ItmpName]): JsObject = fullName.fold(Json.obj()) { found =>
      Json.obj("name" -> (found.givenName.getOrElse("") + " " + found.familyName.getOrElse("")).trim)
    }

    def withATSLink(atsLink: Option[AnnualTaxSummaryLink]): JsObject = atsLink.fold(Json.obj()) { found =>
      Json.obj("annualTaxSummaryLink" -> found)
    }

    def withUtr(utr: Option[Utr]): JsObject = utr.fold(Json.obj())(found => Json.obj("utr" -> found))

    def writes(preFlightCheckResponse: PreFlightCheckResponse): JsObject =
      withNino(preFlightCheckResponse.nino) ++ withSaUtr(preFlightCheckResponse.saUtr) ++ Json
        .obj("routeToIV" -> preFlightCheckResponse.routeToIV) ++ withName(preFlightCheckResponse.name) ++
      withATSLink(preFlightCheckResponse.annualTaxSummaryLink) ++ withUtr(preFlightCheckResponse.utr)
  }

}

case class AnnualTaxSummaryLink(
  link:        String,
  destination: LinkDestination)

object AnnualTaxSummaryLink { implicit val formats: Format[AnnualTaxSummaryLink] = Json.format[AnnualTaxSummaryLink] }

case class Utr(
  saUtr:                SaUtr,
  inactiveEnrolmentUrl: Option[String])

object Utr { implicit val formats: Format[Utr] = Json.format[Utr] }

@ImplementedBy(classOf[LivePreFlightService])
trait PreFlightService[F[_]] {

  def preFlight(
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): F[PreFlightCheckResponse]
}
