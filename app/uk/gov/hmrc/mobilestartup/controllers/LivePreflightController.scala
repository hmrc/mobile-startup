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
import javax.inject.Inject
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, BodyParser, ControllerComponents}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.mobilestartup.services.PreflightService
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.ExecutionContext

case class PreFlightRequest(os: String, version: String)
object PreFlightRequest {
  implicit val formats: OFormat[PreFlightRequest] = Json.format
}

class LivePreflightController @Inject()(
  val controllerComponents:   ControllerComponents,
  preflightService:           PreflightService,
  override val authConnector: AuthConnector
)(
  implicit override val executionContext: ExecutionContext
) extends BackendBaseController
    with AuthorisedFunctions
    with HeaderValidator {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  private val authToken = "AuthToken"

  private val authenticationFailure = new Exception("Failed to resolve authentication from HC!")

  def preFlightCheck(journeyId: Option[String]): Action[PreFlightRequest] =
    validateAccept(acceptHeaderValidationRules).async(controllerComponents.parsers.json[PreFlightRequest]) { implicit request =>
      preflightService.preFlight(request.body, journeyId).map { response =>
        hc.authorization match {
          case Some(auth) => Ok(Json.toJson(response)).addingToSession(authToken -> auth.value)
          case _          => Unauthorized("Failed to resolve authentication from HC!")
        }
      }
    }
}
