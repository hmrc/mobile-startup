/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.mobilestartup

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilestartup.support.BaseISpec
import uk.gov.hmrc.mobilestartup.stubs.AuthStub._
import uk.gov.hmrc.mobilestartup.stubs.AuditStub._
import eu.timepit.refined.auto._

import scala.concurrent.Future

class LiveStartupControllerISpec extends BaseISpec {
  val journeyId: JourneyId = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val url:       String    = s"/startup?journeyId=$journeyId"

  def getRequestWithAcceptHeader(url: String): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader, authorizationJsonHeader).get()

  def postRequestWithAcceptHeader(
    url:  String,
    form: JsValue
  ): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader).post(form)

  def postRequestWithAcceptHeader(url: String): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader).post("")

  def stubRenewalsResponse(): StubMapping =
    stubFor(
      get(urlEqualTo("/income/tax-credits/submission/state/enabled?journeyId=b6ef25bc-8f5e-49c8-98c5-f039f39e4557"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("""
                        |{
                        |  "submissionsState": "open"
                        |}
           """.stripMargin)
        )
    )

  "GET /startup" should {

    "return startup details" in {
      userLoggedIn()
      respondToAuditMergedWithNoBody
      stubRenewalsResponse()

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                                       shouldBe 200
      (response.json \ "feature" \ 0 \ "name").as[String]                   shouldBe "userPanelSignUp"
      (response.json \ "feature" \ 0 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 1 \ "name").as[String]                   shouldBe "helpToSaveEnableBadge"
      (response.json \ "feature" \ 1 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 2 \ "name").as[String]                   shouldBe "enablePushNotificationTokenRegistration"
      (response.json \ "feature" \ 2 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 3 \ "name").as[String]                   shouldBe "paperlessAlertDialogues"
      (response.json \ "feature" \ 3 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 4 \ "name").as[String]                   shouldBe "paperlessAlertDialogs"
      (response.json \ "feature" \ 4 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 5 \ "name").as[String]                   shouldBe "paperlessAdverts"
      (response.json \ "feature" \ 5 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 6 \ "name").as[String]                   shouldBe "htsAdverts"
      (response.json \ "feature" \ 6 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 7 \ "name").as[String]                   shouldBe "annualTaxSummaryLink"
      (response.json \ "feature" \ 7 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "taxCreditRenewals" \ "submissionsState").as[String] shouldBe "open"

    }

    "return 401 when user is not logged in" in {
      userIsNotLoggedIn()

      val response = await(wsUrl(url).addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 401
    }

    "return 403 when user has insufficient confidence level" in {
      userIsLoggedInWithInsufficientConfidenceLevel()

      val response = await(wsUrl(url).addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 403
    }

    "return 400 when no journeyId supplied" in {

      val response =
        await(wsUrl("/preflight-check").addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 400
    }

    "return 400 when invalid journeyId supplied" in {

      val response = await(
        wsUrl("/preflight-check?journeyId=ThisIsAnInvalidJourneyId")
          .addHttpHeaders(acceptJsonHeader)
          .get()
      )
      response.status shouldBe 400
    }
  }
}
