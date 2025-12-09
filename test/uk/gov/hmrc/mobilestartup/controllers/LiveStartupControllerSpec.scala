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

import org.scalamock.handlers.CallHandler
import play.api.http.Status
import play.api.libs.json.JsObject
import play.api.libs.json.Json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.BaseSpec
import uk.gov.hmrc.mobilestartup.connectors.ShutteringConnector
import uk.gov.hmrc.mobilestartup.model.shuttering.{Shuttering, StartupShuttering}
import uk.gov.hmrc.mobilestartup.model.types.JourneyId
import uk.gov.hmrc.mobilestartup.services.StartupService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class LiveStartupControllerSpec extends BaseSpec {
  private val fakeRequest = FakeRequest("GET", "/")

  private val stubStartupService = new StartupService[Future] {

    override def startup(
      nino:               String,
      journeyId:          JourneyId,
      shutteringStatuses: StartupShuttering
    )(implicit hc:        HeaderCarrier
    ): Future[JsObject] =
      Future.successful(obj())
  }

  implicit val mockShutteringConnector: ShutteringConnector = mock[ShutteringConnector]

  def authConnector(stubbedRetrievalResult: Future[?]): AuthConnector = new AuthConnector {

    def authorise[A](
      predicate:   Predicate,
      retrieval:   Retrieval[A]
    )(implicit hc: HeaderCarrier,
      ec:          ExecutionContext
    ): Future[A] =
      stubbedRetrievalResult.map(_.asInstanceOf[A])(ec)
  }

  def mockShutteringResponse(
    response:                     StartupShuttering
  )(implicit shutteringConnector: ShutteringConnector
  ): CallHandler[Future[StartupShuttering]] =
    (shutteringConnector
      .getStartupShuttering(_: JourneyId)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returning(Future successful response)

  "GET /" should {
    "return 200" in {
      mockShutteringResponse(StartupShuttering(Shuttering.shutteringDisabled, Shuttering.shutteringDisabled))

      val controller = new LiveStartupController(
        stubStartupService,
        stubControllerComponents(),
        authConnector(Future.successful(Some("nino"))),
        200,
        mockShutteringConnector
      )
      val result = controller.startup(journeyId)(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
