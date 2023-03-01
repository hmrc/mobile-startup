/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Named, Singleton}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilestartup.services.StartupService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class LiveStartupController @Inject() (
  service:                                             StartupService[Future],
  val controllerComponents:                            ControllerComponents,
  override val authConnector:                          AuthConnector,
  @Named("controllers.confidenceLevel") val confLevel: Int
)(implicit ec:                                         ExecutionContext)
    extends BackendBaseController
    with AuthorisedFunctions {
  class GrantAccessException(message: String) extends HttpException(message, 401)

  def startup(journeyId: JourneyId): Action[AnyContent] =
    Action.async { implicit request =>
      withNinoFromAuth { verifiedNino =>
        service.startup(verifiedNino, journeyId).map(Ok(_))
      }
    }

  /**
    * Check that the user is authorized to at least a confidence level of 200 and retrieve the NINO associated
    * with their account. Run the supplied function with that NINO.
    *
    * Various failure scenarios are translated to appropriate Play `Result` types.
    *
    * @param f - the function to be run with the NINO, if it is successfully retrieved from the auth data
    */
  private def withNinoFromAuth(f: String => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    authConnector
      .authorise(ConfidenceLevel.L200, Retrievals.nino)
      .flatMap {
        case Some(ninoFromAuth) => f(ninoFromAuth)
        case None               => Future.successful(Unauthorized("Authorization failure [user is not enrolled for NI]"))
      }
      .recover {
        case e: NoActiveSession        => Unauthorized(s"Authorisation failure [${e.reason}]")
        case e: GrantAccessException   => Unauthorized(s"Authorisation failure [${e.message}]")
        case e: AuthorisationException => Forbidden(s"Authorisation failure [${e.reason}]")
      }
}
