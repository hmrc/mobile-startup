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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.auth.core.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton()
class LiveStartupController @Inject()(
  connector:                GenericConnector,
  val controllerComponents: ControllerComponents
)(
  implicit ec: ExecutionContext
) extends BackendBaseController {

  def hello(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok("Hello world"))
  }

  def startup(nino: String): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok("Hello world"))
  }

  case class Result(id: String, jsValue: JsValue)

  def buildJourneyQueryParam(journeyId: Option[String]): String = journeyId.fold("")(id => s"?journeyId=$id")
  def logJourneyId(journeyId:           Option[String]) = s"Native Error - ${journeyId.fold("no Journey id supplied")(id => id)}"

  private def taxSummaryStartup(nino: Nino, year: Int)(implicit hc: HeaderCarrier) =
    connector.doGet("mobile-help-to-save", "/mobile-help-to-save/startup", hc) map { json =>
      Some(Result("helpToSave", json))
    } recover {
      case NonFatal(e) =>
        Logger.warn(s"""Exception thrown by "/mobile-help-to-save/startup", not returning any helpToSave result""", e)
        None
    }
}
