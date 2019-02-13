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
import cats.Semigroup
import cats.implicits._
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json.{obj, _}
import play.api.libs.json.{JsObject, JsString, JsValue}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LiveStartupService @Inject()(
  connector: GenericConnector
)(
  implicit ec: ExecutionContext
) extends StartupService {

  implicit val jsonObjectSemigroup: Semigroup[JsObject] = new Semigroup[JsObject] {
    override def combine(x: JsObject, y: JsObject): JsObject = x ++ y
  }

  override def startup(nino: String, journeyId: Option[String])(implicit hc: HeaderCarrier): Future[JsObject] =
    callService("helpToSave")(mhtsStartup) |+|
      callService("taxCreditRenewals")(tcrStartup(journeyId)) |+|
      callService("taxSummary")(taxSummaryStartup(nino, TaxYear.current.currentYear, journeyId))

  private def buildJourneyQueryParam(journeyId: Option[String]): String =
    journeyId.fold("")(id => s"?journeyId=$id")

  private def logJourneyId(journeyId: Option[String]) =
    s"Native Error - ${journeyId.fold("no Journey id supplied")(id => id)}"

  private def callService(name: String)(f: => Future[Option[JsValue]]): Future[JsObject] =
    f.map { result =>
      val json: JsValue = result.getOrElse(obj())
      obj(name -> json)
    }

  private def mhtsStartup(implicit hc: HeaderCarrier): Future[Option[JsValue]] =
    connector
      .doGet("mobile-help-to-save", "/mobile-help-to-save/startup", hc)
      .map(Some(_))
      .recover {
        case NonFatal(e) =>
          Logger.warn(s"""Exception thrown by "/mobile-help-to-save/startup", not returning any helpToSave result""", e)
          None
      }

  private def tcrStartup(journeyId: Option[String])(implicit hc: HeaderCarrier): Future[Option[JsValue]] =
    connector
      .doGet("mobile-tax-credits-renewal", s"/income/tax-credits/submission/state/enabled${buildJourneyQueryParam(journeyId)}", hc)
      .map(res => Some(JsObject(Seq("submissionsState" -> JsString((res \ "submissionsState").as[String])))))
      .recover {
        case NonFatal(e) =>
          Logger.warn(
            s"${logJourneyId(journeyId)} - Failed to retrieve TaxCreditsRenewals and exception is ${e.getMessage}! Default of submissionsState is error!")
          Some(JsObject(Seq("submissionsState" -> JsString("error"))))
      }

  private def taxSummaryStartup(nino: String, year: Int, journeyId: Option[String])(implicit hc: HeaderCarrier): Future[Option[JsValue]] =
    connector
      .doGet("mobile-paye", s"/nino/$nino/tax-year/$year/summary${buildJourneyQueryParam(journeyId)}", hc)
      .map(Some(_))
      .recover {
        case ex: Exception =>
          Logger.warn(s"${logJourneyId(journeyId)} - Failed to retrieve the tax-summary data and exception is ${ex.getMessage}!")
          // An empty JSON object indicates failed to retrieve the tax-summary.
          Some(obj())
      }

}
