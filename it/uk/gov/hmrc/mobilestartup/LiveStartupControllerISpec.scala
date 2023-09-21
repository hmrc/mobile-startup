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

package uk.gov.hmrc.mobilestartup

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.mobilestartup.support.BaseISpec
import uk.gov.hmrc.mobilestartup.stubs.AuthStub._
import uk.gov.hmrc.mobilestartup.stubs.AuditStub._

import scala.concurrent.Future

class LiveStartupControllerISpec extends BaseISpec {
  override val url: String = s"/startup?journeyId=$journeyId"

  def getRequestWithAuthHeaders(url: String): Future[WSResponse] =
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

  def stubCitizenDetailsResponse(): StubMapping =
    stubFor(
      get(urlEqualTo("/citizen-details/AA000006C/designatory-details"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("""{
                        |    "person": {
                        |      "firstName": "Angus",
                        |      "middleName": "John",
                        |      "lastName": "Smith",
                        |      "title": "Mr",
                        |      "honours": null,
                        |      "sex": "M",
                        |      "dateOfBirth": -26092800000,
                        |      "nino": "AA000006C"
                        |    },
                        |    "address": {
                        |      "line1": "123456",
                        |      "line2": "23456",
                        |      "line3": "3456",
                        |      "line4": "456",
                        |      "line5": "55555",
                        |      "postcode": "98765",
                        |      "startDate": 946684800000,
                        |      "country": "Test Country",
                        |      "type": "Residential"
                        |    },
                        |    "correspondenceAddress": {
                        |      "line1": "1 Main Street",
                        |      "line2": "Central",
                        |      "line3": "Anothertown",
                        |      "line4": "Anothershire",
                        |      "line5": "Anotherline",
                        |      "postcode": "AA1 1AA",
                        |      "startDate": 1341100800000,
                        |      "country": null,
                        |      "type": "Correspondence"
                        |    }
                        |  }
                        |""".stripMargin.stripMargin)
        )
    )

  "GET /startup" should {

    "return startup details" in {
      userLoggedIn()
      respondToAuditMergedWithNoBody
      stubRenewalsResponse()
      stubCitizenDetailsResponse()

      val response = await(getRequestWithAuthHeaders(url))

      response.status                                         shouldBe 200
      (response.json \ "feature" \ 0 \ "name").as[String]     shouldBe "userPanelSignUp"
      (response.json \ "feature" \ 0 \ "enabled").as[Boolean] shouldBe true
      (response.json \ "feature" \ 1 \ "name").as[String]     shouldBe "enablePushNotificationTokenRegistration"
      (response.json \ "feature" \ 1 \ "enabled").as[Boolean] shouldBe true
      (response.json \ "feature" \ 2 \ "name").as[String]     shouldBe "paperlessAlertDialogs"
      (response.json \ "feature" \ 2 \ "enabled").as[Boolean] shouldBe true
      (response.json \ "feature" \ 3 \ "name").as[String]     shouldBe "paperlessAdverts"
      (response.json \ "feature" \ 3 \ "enabled").as[Boolean] shouldBe true
      (response.json \ "feature" \ 4 \ "name").as[String]     shouldBe "htsAdverts"
      (response.json \ "feature" \ 4 \ "enabled").as[Boolean] shouldBe true
      (response.json \ "feature" \ 5 \ "name").as[String]     shouldBe "annualTaxSummaryLink"
      (response.json \ "feature" \ 5 \ "enabled").as[Boolean] shouldBe true
      (response.json \ "feature" \ 6 \ "name").as[String]     shouldBe "payeCustomerSatisfactionSurveyAdverts"
      (response.json \ "feature" \ 6 \ "enabled").as[Boolean] shouldBe true
      (response.json \ "feature" \ 7 \ "name")
        .as[String]                                           shouldBe "selfAssessmentPaymentsCustomerSatisfactionSurveyAdverts"
      (response.json \ "feature" \ 7 \ "enabled").as[Boolean] shouldBe true
      (response.json \ "feature" \ 8 \ "name")
        .as[String]                                                         shouldBe "customerSatisfactionSurveys"
      (response.json \ "feature" \ 8 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 9 \ "name").as[String]                   shouldBe "findMyNinoAddToWallet"
      (response.json \ "feature" \ 9 \ "enabled").as[Boolean]               shouldBe true
      (response.json \ "feature" \ 10 \ "name").as[String]                  shouldBe "disableYourEmploymentIncomeChart"
      (response.json \ "feature" \ 10 \ "enabled").as[Boolean]              shouldBe false
      (response.json \ "feature" \ 11 \ "name").as[String]                  shouldBe "disableYourEmploymentIncomeChartAndroid"
      (response.json \ "feature" \ 11 \ "enabled").as[Boolean]              shouldBe false
      (response.json \ "feature" \ 12 \ "name").as[String]                  shouldBe "findMyNinoAddToGoogleWallet"
      (response.json \ "feature" \ 12 \ "enabled").as[Boolean]              shouldBe true
      (response.json \ "taxCreditRenewals" \ "submissionsState").as[String] shouldBe "open"
      (response.json \ "user" \ "name").as[String]                          shouldBe "Angus John Smith"
      (response.json \ "user" \ "address" \ "line1").as[String]             shouldBe "123456"
      (response.json \ "user" \ "address" \ "line2").as[String]             shouldBe "23456"
      (response.json \ "user" \ "address" \ "line3").as[String]             shouldBe "3456"
      (response.json \ "user" \ "address" \ "line4").as[String]             shouldBe "456"
      (response.json \ "user" \ "address" \ "line5").as[String]             shouldBe "55555"
      (response.json \ "user" \ "address" \ "postcode").as[String]          shouldBe "98765"
      (response.json \ "user" \ "address" \ "country").as[String]           shouldBe "Test Country"
      (response.json \ "urls" \ 0 \ "name").as[String]                      shouldBe "cbProofOfEntitlementUrl"
      (response.json \ "urls" \ 0 \ "url").as[String]                       shouldBe "/child-benefit/view-proof-entitlement"
      (response.json \ "urls" \ 1 \ "name").as[String]                      shouldBe "cbProofOfEntitlementUrlCy"
      (response.json \ "urls" \ 1 \ "url").as[String]                       shouldBe "/child-benefit/view-proof-entitlementCy"
      (response.json \ "urls" \ 2 \ "name").as[String]                      shouldBe "cbPaymentHistoryUrl"
      (response.json \ "urls" \ 2 \ "url").as[String]                       shouldBe "/child-benefit/view-payment-history"
      (response.json \ "urls" \ 3 \ "name").as[String]                      shouldBe "cbPaymentHistoryUrlCy"
      (response.json \ "urls" \ 3 \ "url").as[String]                       shouldBe "/child-benefit/view-payment-historyCy"
      (response.json \ "urls" \ 4 \ "name").as[String]                      shouldBe "cbHomeUrl"
      (response.json \ "urls" \ 4 \ "url").as[String]                       shouldBe "/child-benefit/home"
      (response.json \ "urls" \ 5 \ "name").as[String]                      shouldBe "cbHomeUrlCy"
      (response.json \ "urls" \ 5 \ "url").as[String]                       shouldBe "/child-benefit/homeCy"
      (response.json \ "urls" \ 6 \ "name").as[String]                      shouldBe "cbHowToClaimUrl"
      (response.json \ "urls" \ 6 \ "url").as[String]                       shouldBe "/child-benefit/how-to-claim"
      (response.json \ "urls" \ 7 \ "name").as[String]                      shouldBe "cbHowToClaimUrlCy"
      (response.json \ "urls" \ 7 \ "url").as[String]                       shouldBe "/child-benefit/how-to-claimCy"
      (response.json \ "urls" \ 8 \ "name").as[String]                      shouldBe "cbFullTimeEducationUrl"
      (response.json \ "urls" \ 8 \ "url").as[String]                       shouldBe "/gov-uk/child-benefit-16-19"
      (response.json \ "urls" \ 9 \ "name").as[String]                      shouldBe "cbFullTimeEducationUrlCy"
      (response.json \ "urls" \ 9 \ "url").as[String]                       shouldBe "/gov-uk/child-benefit-16-19Cy"
      (response.json \ "urls" \ 10 \ "name").as[String]                     shouldBe "cbWhatChangesUrl"
      (response.json \ "urls" \ 10 \ "url").as[String]                      shouldBe "/personal-account/child-benefit-forms"
      (response.json \ "urls" \ 11 \ "name").as[String]                     shouldBe "cbWhatChangesUrlCy"
      (response.json \ "urls" \ 11 \ "url").as[String]                      shouldBe "/personal-account/child-benefit-formsCy"
      (response.json \ "urls" \ 12 \ "name").as[String]                     shouldBe "statePensionUrl"
      (response.json \ "urls" \ 12 \ "url").as[String]                      shouldBe "/statePensionUrl"
      (response.json \ "urls" \ 13 \ "name").as[String]                     shouldBe "niSummaryUrl"
      (response.json \ "urls" \ 13 \ "url").as[String]                      shouldBe "/niSummaryUrl"
      (response.json \ "urls" \ 14 \ "name").as[String]                     shouldBe "niContributionsUrl"
      (response.json \ "urls" \ 14 \ "url").as[String]                      shouldBe "/niContributionsUrl"
      (response.json \ "urls" \ 15 \ "name").as[String]                     shouldBe "otherTaxesDigitalAssistantUrl"
      (response.json \ "urls" \ 15 \ "url").as[String]                      shouldBe "/otherTaxesDigitalAssistantUrl"
      (response.json \ "urls" \ 16 \ "name").as[String]                     shouldBe "otherTaxesDigitalAssistantUrlCy"
      (response.json \ "urls" \ 16 \ "url").as[String]                      shouldBe "/otherTaxesDigitalAssistantUrlCy"
    }

    "return 401 when user is not logged in" in {
      userIsNotLoggedIn()

      val response = await(wsUrl(url).addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 401
    }

    "return 401 when no nino is found for user" in {
      userLoggedInNoNino()

      val response = await(wsUrl(url).addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 401
    }

    "return 403 when user has insufficient confidence level" in {
      userIsLoggedInWithInsufficientConfidenceLevel()

      val response = await(wsUrl(url).addHttpHeaders(acceptJsonHeader, authorizationJsonHeader).get())
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

    "return 401 when no accept header is supplied" in {
      userIsNotLoggedIn()
      val response = await(wsUrl(url).addHttpHeaders(authorizationJsonHeader).get())
      response.status shouldBe 401
    }

    "return 401 when no bearerToken is supplied" in {
      userIsNotLoggedIn()
      val response = await(wsUrl(url).addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 401
    }
  }
}
