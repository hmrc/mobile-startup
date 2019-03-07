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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.ExecutionContext

class SandboxStartupController @Inject()(
  val controllerComponents: ControllerComponents
)(
  implicit ec: ExecutionContext
) extends BackendBaseController
    with FileResource {

  def startup(journeyId: Option[String]): Action[AnyContent] = Action { _ =>
    Ok(Json.parse(findResource("/sandbox/startup.json").getOrElse("{}")))
  }
}
