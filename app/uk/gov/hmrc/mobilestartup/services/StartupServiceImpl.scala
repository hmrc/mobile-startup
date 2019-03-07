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
import cats.MonadError
import cats.implicits._
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.time.TaxYear

import scala.util.control.NonFatal

case class FeatureFlag(name: String, enabled: Boolean)

object FeatureFlag {
  implicit val formats: Format[FeatureFlag] = Json.format[FeatureFlag]
}

/**
  * Decided to implement this generically using Tagless as an example of how it can be introduced
  * into a codebase without necessarily converting everything. It did require introducing a type parameter
  * onto the `GenericConnector` trait but that had very little impact beyond the change to the guice wiring.
  */
class StartupServiceImpl[F[_]] @Inject()(
  connector:            GenericConnector[F],
  payAsYouEarnOnDemand: Boolean
)(
  implicit F: MonadError[F, Throwable]
) extends StartupService[F] {

  override def startup(nino: String, journeyId: Option[String])(implicit hc: HeaderCarrier): F[JsObject] =
    (
      callService("helpToSave")(mhtsStartup),
      callService("taxCreditRenewals")(tcrStartup(journeyId)),
      callService("taxSummary")(taxSummaryStartup(nino, TaxYear.current.currentYear, journeyId)),
      featureFlags.pure[F]).mapN((a, b, c, d) => a ++ b ++ c ++ d)

  private val featureFlags: JsObject =
    obj("feature" -> List(FeatureFlag("payAsYouEarnOnDeman", payAsYouEarnOnDemand)))

  private def buildJourneyQueryParam(journeyId: Option[String]): String =
    journeyId.fold("")(id => s"?journeyId=$id")

  private def logJourneyId(journeyId: Option[String]) =
    s"Native Error - ${journeyId.fold("no Journey id supplied")(id => id)}"

  private def callService(name: String)(f: => F[Option[JsValue]]): F[JsObject] =
    // If the service call returns a valid result or an error then map it into the object against
    // the supplied name, but if the result is None then just return an empty object so that the
    // services section will not appear in the final result at all.
    f.map {
      case Some(json) => obj(name -> json)
      case None       => obj()
    }

  private def mhtsStartup(implicit hc: HeaderCarrier): F[Option[JsValue]] =
    connector
      .doGet("mobile-help-to-save", "/mobile-help-to-save/startup", hc)
      .map(_.some)
      .recover {
        case NonFatal(e) =>
          Logger.warn(s"""Exception thrown by "/mobile-help-to-save/startup", not returning any helpToSave result""", e)
          obj().some
      }

  private def tcrStartup(journeyId: Option[String])(implicit hc: HeaderCarrier): F[Option[JsValue]] =
    connector
      .doGet("mobile-tax-credits-renewal", s"/income/tax-credits/submission/state/enabled${buildJourneyQueryParam(journeyId)}", hc)
      .map[Option[JsValue]](res => obj("submissionsState" -> JsString((res \ "submissionsState").as[String])).some)
      .recover {
        case NonFatal(e) =>
          Logger.warn(
            s"${logJourneyId(journeyId)} - Failed to retrieve TaxCreditsRenewals and exception is ${e.getMessage}! Default of submissionsState is error!")
          obj("submissionsState" -> JsString("error")).some
      }

  private def taxSummaryStartup(nino: String, year: Int, journeyId: Option[String])(implicit hc: HeaderCarrier): F[Option[JsValue]] =
    if (payAsYouEarnOnDemand) none[JsValue].pure[F]
    else
      connector
        .doGet("mobile-paye", s"/nino/$nino/tax-year/$year/summary${buildJourneyQueryParam(journeyId)}", hc)
        .map(_.some)
        .recover {
          case ex: Exception =>
            Logger.warn(s"${logJourneyId(journeyId)} - Failed to retrieve the tax-summary data and exception is ${ex.getMessage}!")
            // An empty JSON object indicates failed to retrieve the tax-summary.
            obj().some
        }
}
