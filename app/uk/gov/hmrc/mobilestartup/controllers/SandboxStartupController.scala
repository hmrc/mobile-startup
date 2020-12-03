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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, BodyParser, ControllerComponents}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

class SandboxStartupController @Inject()(val controllerComponents: ControllerComponents)(implicit val ec: ExecutionContext)
  extends BackendBaseController
    with FileResource
    with HeaderValidator {

  def startup(journeyId: JourneyId): Action[AnyContent] = {
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      val sandboxControl: Option[String] = request.headers.get("SANDBOX-CONTROL")

      Future.successful {
        sandboxControl match {
          case Some("ERROR-401") => Unauthorized
          case Some("ERROR-403") => Forbidden
          case Some("ERROR-500") => InternalServerError
          case Some("RENEWALS-OPEN") => Ok(readData("startup.json"))
          case Some("RENEWALS-VIEW-ONLY") => Ok(readData("startup.json", renewalsStatus = "status_view_only"))
          case Some("RENEWALS-CLOSED") => Ok(readData("startup.json", renewalsStatus = "closed"))
          case Some("HTS-ENROLLED") => Ok(readData("startup.json"))
          case Some("HTS-ELIGABLE") => Ok(readData("startup.json", htsStatus = "NotEnrolledButEligable"))
          case Some("HTS-NOT-ENROLLED") => Ok(readData("startup.json", htsStatus = "NotEnrolled"))
          case _ => Ok(readData("startup.json"))
        }
      }
    }
  }

  private def readData(resource: String, renewalsStatus: String = "open", htsStatus: String = "Enrolled") = {
          findResource(s"/resources/mobilepayesummary/$resource")
            .getOrElse(throw new IllegalArgumentException("Resource not found!"))
            .replace("<RENEWALS_STATUS>", renewalsStatus)
            .replace("<HTS_STATUS>", htsStatus)
  }

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent
}
