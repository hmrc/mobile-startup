/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.mobilestartup.BaseSpec
import uk.gov.hmrc.mobilestartup.services.{AnnualTaxSummaryLink, LivePreFlightService, PreFlightCheckResponse, PreFlightService, Utr}
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.RateLimitedException
import uk.gov.hmrc.mobilestartup.model.types.JourneyId

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilestartup.model.Activated
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.fromStringtoLinkDestination

class LivePreFlightControllerSpec extends BaseSpec {

  private val fakeRequest = FakeRequest("GET", "/")
    .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  private val preflightService = mock[PreFlightService[Future]]
  private val authConnector    = mock[AuthConnector]
  private val enablePertax     = false

  val controller = LivePreFlightController(
    stubControllerComponents(),
    preflightService,
    authConnector,
    enablePertax
  )

  "/preflight-check" should {

    "return TooManyRequests(429) response when the citizen details request is rate limited" in {

      (preflightService.preFlight(_: JourneyId, _: Boolean)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.failed(RateLimitedException()))

      val result = controller.preFlightCheck(journeyId)(fakeRequest)
      status(result) shouldBe 429
    }

    "return Unauthorized(401) response when authorization header is missing" in {

      val preflightResponse = PreFlightCheckResponse(
        Some(Nino("CS700100A")),
        routeToIV = false,
        Some(AnnualTaxSummaryLink("/", fromStringtoLinkDestination("PAYE"))),
        Some(Utr(saUtr = Some(SaUtr("1234567890")), status = Activated)),
        Enrolments(Set.empty),
        demoAccount = true,
        isEligible = true
      )

      (preflightService.preFlight(_: JourneyId, _: Boolean)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(preflightResponse))

      val result = controller.preFlightCheck(journeyId)(fakeRequest)
      status(result) shouldBe 401
    }

    "return Ok(200) response with expected body when authorization header is preset" in {

      val preflightResponse = PreFlightCheckResponse(
        Some(Nino("CS700100A")),
        routeToIV = false,
        Some(AnnualTaxSummaryLink("/", fromStringtoLinkDestination("PAYE"))),
        Some(Utr(saUtr = Some(SaUtr("1234567890")), status = Activated)),
        Enrolments(Set.empty),
        demoAccount = true,
        isEligible = true
      )

      (preflightService.preFlight(_: JourneyId, _: Boolean)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(preflightResponse))

      val result = controller.preFlightCheck(journeyId)(fakeRequest.withHeaders(
        "Authorization" -> "auth-header-value"
      ))
      status(result) shouldBe 200
    }
  }

}
