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

import javax.inject.{Inject, Named, Singleton}
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.services.StartupService
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class LiveStartupController @Inject()(
  service:                                             StartupService,
  val controllerComponents:                            ControllerComponents,
  override val authConnector:                          AuthConnector,
  @Named("controllers.confidenceLevel") val confLevel: Int
)(
  implicit ec: ExecutionContext
) extends BackendBaseController
    with AuthorisedFunctions {

  def startup(journeyId: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      withAuth(service.startup(_, journeyId).map(Ok(_)))
    }

  private def withAuth(f: String => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    lazy val ninoNotFoundOnAccount = new NinoNotFoundOnAccount
    lazy val lowConfidenceLevel    = new AccountWithLowCL

    authorised(Enrolment("HMRC-NI", Nil, "Activated", None))
      .retrieve(nino and confidenceLevel) {
        case Some(foundNino) ~ foundConfidenceLevel =>
          if (foundNino.isEmpty) throw ninoNotFoundOnAccount
          if (confLevel > foundConfidenceLevel.level) throw lowConfidenceLevel
          f(foundNino)
      }
  }
}
