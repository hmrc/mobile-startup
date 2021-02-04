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

package uk.gov.hmrc.mobilestartup.services
import cats.implicits._
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.{BaseSpec, TestF}

class StartupServiceImplSpec extends BaseSpec with TestF {

  private val helpToSave         = "helpToSave"
  private val taxCreditsRenewals = "taxCreditRenewals"
  private val messages           = "messages"
  val successfulResponse         = JsString("success")

  private val htsSuccessResponse:      JsValue = successfulResponse
  private val tcrSuccessResponse:      JsValue = obj("submissionsState" -> "open")
  private val messagesSuccessResponse: JsValue = Json.parse("""{
                                                              |  "paye": [
                                                              |    {
                                                              |      "type": "Info",
                                                              |      "id": "paye-message-1",
                                                              |      "headline": "Title2 - Has active date",
                                                              |      "content": {
                                                              |        "title": "title Content title",
                                                              |        "body": "Content2"
                                                              |      },
                                                              |      "activeWindow": {
                                                              |        "startTime": "2020-03-01T20:06:12.726",
                                                              |        "endTime": "2020-05-24T20:06:12.726"
                                                              |      }
                                                              |    }
                                                              |  ],
                                                              |  "tc": [
                                                              |    {
                                                              |      "type": "Info",
                                                              |      "id": "tc-message-1",
                                                              |      "headline": "Title2 - Has active date",
                                                              |      "content": {
                                                              |        "title": "title Content title",
                                                              |        "body": "Content2"
                                                              |      },
                                                              |      "activeWindow": {
                                                              |        "startTime": "2020-03-01T20:06:12.726",
                                                              |        "endTime": "2020-05-24T20:06:12.726"
                                                              |      }
                                                              |    }
                                                              |  ],
                                                              |  "hts": [
                                                              |    {
                                                              |      "type": "Warning",
                                                              |      "id": "hts-message-1",
                                                              |      "headline": "Title3",
                                                              |      "content": {
                                                              |        "body": "Content3"
                                                              |      },
                                                              |      "link": {
                                                              |        "url": "URL3",
                                                              |        "urlType": "Normal",
                                                              |        "type": "Secondary",
                                                              |        "message": "Click me"
                                                              |      }
                                                              |    },
                                                              |    {
                                                              |      "type": "Urgent",
                                                              |      "id": "hts-message-2",
                                                              |      "headline": "Title4",
                                                              |      "content": {
                                                              |        "body": "Content4"
                                                              |      },
                                                              |      "link": {
                                                              |        "url": "URL4",
                                                              |        "urlType": "Normal",
                                                              |        "type": "Secondary",
                                                              |        "message": "Click me"
                                                              |      }
                                                              |    }
                                                              |  ]
                                                              |}
                                                              |""".stripMargin)

  private def dummyConnector(
    htsResponse:           TestF[JsValue] = htsSuccessResponse.pure[TestF],
    tcrResponse:           TestF[JsValue] = tcrSuccessResponse.pure[TestF],
    inAppMessagesResponse: TestF[JsValue] = messagesSuccessResponse.pure[TestF]
  ): GenericConnector[TestF] =
    new GenericConnector[TestF] {

      override def doGet(
        serviceName: String,
        path:        String,
        hc:          HeaderCarrier
      ): TestF[JsValue] =
        serviceName match {
          case "mobile-help-to-save"        => htsResponse
          case "mobile-tax-credits-renewal" => tcrResponse
          case "mobile-in-app-messages"     => inAppMessagesResponse
          case _                            => obj().pure[TestF]
        }

      override def doPost[T](
        json:         JsValue,
        serviceName:  String,
        path:         String,
        hc:           HeaderCarrier
      )(implicit rds: HttpReads[T]
      ): TestF[T] = ???
    }

  "a fully successful response" should {
    "contain success entries for each service" in {
      val sut = new StartupServiceImpl[TestF](dummyConnector(),
                                              userPanelSignUp                         = false,
                                              helpToSaveEnableBadge                   = true,
                                              enablePushNotificationTokenRegistration = false,
                                              enablePaperlessAlertDialogues           = false,
                                              enablePaperlessAdverts                  = false,
                                              enableHtsAdverts                        = false,
                                              enableAnnualTaxSummaryLink              = false)

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                         = false),
        FeatureFlag("helpToSaveEnableBadge", enabled                   = true),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled = false),
        FeatureFlag("paperlessAlertDialogues", enabled                 = false),
        FeatureFlag("paperlessAdverts", enabled                        = false),
        FeatureFlag("htsAdverts", enabled                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                    = false)
      )
      (result \ messages).toOption.value shouldBe messagesSuccessResponse
    }
  }

  "a response" should {
    " not contain an entry for help-to-save when the hts call fails" in {
      val sut = new StartupServiceImpl[TestF](dummyConnector(htsResponse = new Exception("hts failed").error),
                                              false,
                                              helpToSaveEnableBadge                   = true,
                                              enablePushNotificationTokenRegistration = false,
                                              enablePaperlessAlertDialogues           = false,
                                              enablePaperlessAdverts                  = false,
                                              enableHtsAdverts                        = false,
                                              enableAnnualTaxSummaryLink              = false)

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption               shouldBe None
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                         = false),
        FeatureFlag("helpToSaveEnableBadge", enabled                   = true),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled = false),
        FeatureFlag("paperlessAlertDialogues", enabled                 = false),
        FeatureFlag("paperlessAdverts", enabled                        = false),
        FeatureFlag("htsAdverts", enabled                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                    = false)
      )
    }

    "contain an error entry for tcr when the tcr call fails" in {
      val sut = new StartupServiceImpl[TestF](dummyConnector(tcrResponse = new Exception("tcr failed").error),
                                              false,
                                              helpToSaveEnableBadge                   = true,
                                              enablePushNotificationTokenRegistration = false,
                                              enablePaperlessAlertDialogues           = false,
                                              enablePaperlessAdverts                  = false,
                                              enableHtsAdverts                        = false,
                                              enableAnnualTaxSummaryLink              = false)

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe obj("submissionsState" -> "error")
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                         = false),
        FeatureFlag("helpToSaveEnableBadge", enabled                   = true),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled = false),
        FeatureFlag("paperlessAlertDialogues", enabled                 = false),
        FeatureFlag("paperlessAdverts", enabled                        = false),
        FeatureFlag("htsAdverts", enabled                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                    = false)
      )
    }

    "contain an empty lists entry for messages when the messages call fails" in {
      val sut = new StartupServiceImpl[TestF](
        dummyConnector(inAppMessagesResponse = new Exception("message call failed").error),
        false,
        helpToSaveEnableBadge                   = true,
        enablePushNotificationTokenRegistration = false,
        enablePaperlessAlertDialogues           = false,
        enablePaperlessAdverts                  = false,
        enableHtsAdverts                        = false,
        enableAnnualTaxSummaryLink              = false
      )

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe obj("submissionsState" -> "open")
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                         = false),
        FeatureFlag("helpToSaveEnableBadge", enabled                   = true),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled = false),
        FeatureFlag("paperlessAlertDialogues", enabled                 = false),
        FeatureFlag("paperlessAdverts", enabled                        = false),
        FeatureFlag("htsAdverts", enabled                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                    = false)
      )
      (result \ messages).toOption.value shouldBe Json.parse("""{
                                                               |  "paye": [],
                                                               |  "tc": [],
                                                               |  "hts": [],
                                                               |  "tcp": []
                                                               |}
                                                               |""".stripMargin)
    }
  }
}
