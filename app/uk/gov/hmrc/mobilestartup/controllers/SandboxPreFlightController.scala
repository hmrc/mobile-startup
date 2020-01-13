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

package uk.gov.hmrc.mobilestartup.controllers
import javax.inject.Inject
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilestartup.services.PreFlightCheckResponse
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

class SandboxPreFlightController @Inject()(
  val controllerComponents: ControllerComponents
)(
  implicit val executionContext: ExecutionContext
) extends BackendBaseController
    with FileResource
    with HeaderValidator {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  def preFlightCheck(journeyId: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      val sandboxControl: Option[String] = request.headers.get("SANDBOX-CONTROL")

      Future.successful {
        sandboxControl match {
          case Some("ERROR-401") => Unauthorized
          case Some("ERROR-403") => Forbidden
          case Some("ERROR-500") => InternalServerError
          case _ =>
            val (upgrade, toIV) = sandboxControl match {
              case Some("ROUTE-TO-IV")         => (false, true)
              case Some("ROUTE-TO-TWO-FACTOR") => (false, false)
              case _                           => (false, false)
            }

            Ok(toJson(buildPreFlightResponse(upgrade, toIV)))
        }
      }
    }

  def buildPreFlightResponse(upgrade: Boolean, toIV: Boolean): PreFlightCheckResponse =
    PreFlightCheckResponse(Some(Nino("CS700100A")), Some(SaUtr("1234567890")), toIV)
}
