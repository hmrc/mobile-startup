/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.model.{EnrolmentStatus, NoUtr}
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.{JourneyId, LinkDestination}
import uk.gov.hmrc.mobilestartup.model.types._

import scala.concurrent.ExecutionContext

case class PreFlightCheckResponse(
  nino:                 Option[Nino],
  saUtr:                Option[SaUtr],
  credId:               Option[String],
  routeToIV:            Boolean,
  annualTaxSummaryLink: Option[AnnualTaxSummaryLink] = None,
  utr:                  Option[Utr],
  enrolments:           Enrolments,
  routeToTEN:           Boolean = false)

object PreFlightCheckResponse {

  implicit val writes: Writes[PreFlightCheckResponse] = new Writes[PreFlightCheckResponse] {

    def withNino(nino: Option[Nino]): JsObject = nino.fold(Json.obj()) { found =>
      Json.obj("nino" -> found.value)
    }

    def withATSLink(atsLink: Option[AnnualTaxSummaryLink]): JsObject = atsLink.fold(Json.obj()) { found =>
      Json.obj("annualTaxSummaryLink" -> found)
    }

    def withUtr(utr: Option[Utr]): JsObject = utr.fold(Json.obj())(found => Json.obj("utr" -> found))

    def withCredId(credId: Option[String]): JsObject = credId.fold(Json.obj()) { found =>
      Json.obj("credId" -> found)
    }

    def writes(preFlightCheckResponse: PreFlightCheckResponse): JsObject =
      withNino(preFlightCheckResponse.nino) ++ Json
        .obj("routeToIV" -> preFlightCheckResponse.routeToIV) ++
      withATSLink(preFlightCheckResponse.annualTaxSummaryLink) ++ withUtr(preFlightCheckResponse.utr) ++ Json
        .obj("routeToTEN" -> preFlightCheckResponse.routeToTEN) ++ withCredId(preFlightCheckResponse.credId)
  }

}

case class AnnualTaxSummaryLink(
  link:        String,
  destination: LinkDestination)

object AnnualTaxSummaryLink { implicit val formats: Format[AnnualTaxSummaryLink] = Json.format[AnnualTaxSummaryLink] }

case class Utr(
  saUtr:                Option[SaUtr],
  status:               EnrolmentStatus,
  inactiveEnrolmentUrl: Option[String])

object Utr {

  def apply(
    saUtr:  Option[SaUtr],
    status: EnrolmentStatus
  ): Utr = Utr(saUtr, status, status.link)

  val noUtr: Utr = Utr(None, NoUtr, NoUtr.link)

  implicit val formats: Format[Utr] = Json.format[Utr]

}

@ImplementedBy(classOf[LivePreFlightService])
trait PreFlightService[F[_]] {

  def preFlight(
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): F[PreFlightCheckResponse]
}
