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

package uk.gov.hmrc.mobilestartup.controllers
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BodyParser, ControllerComponents}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilestartup.services.PreFlightService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

class LivePreFlightController @Inject() (
  val controllerComponents:               ControllerComponents,
  preflightService:                       PreFlightService[Future],
  override val authConnector:             AuthConnector
)(implicit override val executionContext: ExecutionContext)
    extends BackendBaseController
    with AuthorisedFunctions
    with HeaderValidator {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  def preFlightCheck(journeyId: JourneyId): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      preflightService.preFlight(journeyId).map { response =>
        hc.authorization match {
          case Some(_) => Ok(Json.toJson(response))
          case _       => Unauthorized("Failed to resolve authentication from HC!")
        }
      }
    }
}
