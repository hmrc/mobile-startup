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

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.domain.{DeviceVersion, VersionCheckResponse}
import uk.gov.hmrc.mobilestartup.services.VersionCheckService
import uk.gov.hmrc.play.bootstrap.controller.{BackendBaseController, BackendController}

import scala.concurrent.{ExecutionContext, Future}

trait VersionCheckController extends BackendBaseController with HeaderValidator {
  implicit def executionContext: ExecutionContext

  def versionCheck(journeyId: Option[String] = None): Action[JsValue] =
    validateAccept(acceptHeaderValidationRules).async(controllerComponents.parsers.json) { implicit request =>
      request.body
        .validate[DeviceVersion]
        .fold(
          errors => {
            Logger.warn("Received error with service validate app version: " + errors)
            Future.successful(BadRequest(JsError.toJson(errors)))
          },
          deviceVersion => {
            doVersionCheck(deviceVersion, journeyId)
          }
        )
    }

  def doVersionCheck(deviceVersion: DeviceVersion, journeyId: Option[String])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result]
}

@Singleton
class LiveVersionCheckController @Inject()(
  val service:                   VersionCheckService,
  cc:                            ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends BackendController(cc)
    with VersionCheckController {
  override def parser: BodyParser[AnyContent] = cc.parsers.anyContent

  override def doVersionCheck(deviceVersion: DeviceVersion, journeyId: Option[String])(
    implicit hc:                             HeaderCarrier,
    request:                                 Request[_]): Future[Result] =
    service.versionCheck(deviceVersion, journeyId).map { upgradeRequired =>
      Ok(Json.toJson(VersionCheckResponse(upgradeRequired)))
    }
}

@Singleton
class SandboxVersionCheckController @Inject()(
  cc: ControllerComponents
)(
  implicit val executionContext: ExecutionContext
) extends BackendController(cc)
    with VersionCheckController {
  override def parser: BodyParser[AnyContent] = cc.parsers.anyContent

  override def doVersionCheck(deviceVersion: DeviceVersion, journeyId: Option[String])(
    implicit hc:                             HeaderCarrier,
    request:                                 Request[_]): Future[Result] = {

    val result: Result = request.headers.get("SANDBOX-CONTROL") match {
      case Some("ERROR-500")        => InternalServerError
      case Some("UPGRADE-REQUIRED") => Ok(Json.toJson(VersionCheckResponse(true)))
      case _                        => Ok(Json.toJson(VersionCheckResponse(false)))
    }

    Future successful result
  }
}
