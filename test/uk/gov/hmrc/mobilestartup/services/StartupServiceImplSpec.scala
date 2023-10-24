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

package uk.gov.hmrc.mobilestartup.services
import cats.implicits._
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.{CidPerson, EnrolmentStoreResponse}
import uk.gov.hmrc.mobilestartup.{BaseSpec, TestF}

class StartupServiceImplSpec extends BaseSpec with TestF {

  private val helpToSave         = "helpToSave"
  private val taxCreditsRenewals = "taxCreditRenewals"
  private val messages           = "messages"
  private val user               = "user"
  private val successfulResponse = JsString("success")

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

  private val citizenDetailsSuccessResponse: JsValue = Json.parse("""{
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
                                                                    |""".stripMargin)

  private val userExpectedResponse: JsValue = Json.parse("""{
                                                           |    "name": "Angus John Smith",
                                                           |    "address": {
                                                           |      "line1": "123456",
                                                           |      "line2": "23456",
                                                           |      "line3": "3456",
                                                           |      "line4": "456",
                                                           |      "line5": "55555",
                                                           |      "postcode": "98765",
                                                           |      "country": "Test Country"
                                                           |    }
                                                           |  }
                                                           |""".stripMargin)

  private def dummyConnector(
    htsResponse:            TestF[JsValue] = htsSuccessResponse.pure[TestF],
    tcrResponse:            TestF[JsValue] = tcrSuccessResponse.pure[TestF],
    inAppMessagesResponse:  TestF[JsValue] = messagesSuccessResponse.pure[TestF],
    citizenDetailsResponse: TestF[JsValue] = citizenDetailsSuccessResponse.pure[TestF]
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
          case "citizen-details"            => citizenDetailsResponse
          case _                            => obj().pure[TestF]
        }

      override def cidGet(
        serviceName: String,
        path:        String,
        hc:          HeaderCarrier
      ): TestF[CidPerson] = ???

      override def enrolmentStoreGet(
        serviceName: String,
        path:        String,
        hc:          HeaderCarrier
      ): TestF[EnrolmentStoreResponse] = ???

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
      val sut =
        new StartupServiceImpl[TestF](dummyConnector(),
                                      userPanelSignUp                                        = false,
                                      enablePushNotificationTokenRegistration                = false,
                                      enablePaperlessAlertDialogs                            = false,
                                      enablePaperlessAdverts                                 = false,
                                      enableHtsAdverts                                       = false,
                                      enableAnnualTaxSummaryLink                             = false,
                                      cbProofOfEntitlementUrl                                = Some("/cb/cbProofOfEntitlementUrl"),
                                      cbProofOfEntitlementUrlCy                              = Some("/cb/cbProofOfEntitlementUrlCy"),
                                      cbPaymentHistoryUrl                                    = Some("/cb/cbPaymentHistoryUrl"),
                                      cbPaymentHistoryUrlCy                                  = Some("/cb/cbPaymentHistoryUrlCy"),
                                      cbChangeBankAccountUrl                                 = Some("/cb/cbChangeBankAccountUrl"),
                                      cbChangeBankAccountUrlCy                               = Some("/cb/cbChangeBankAccountUrlCy"),
                                      cbHomeUrl                                              = Some("/cb/cbHomeUrl"),
                                      cbHomeUrlCy                                            = Some("/cb/cbHomeUrlCy"),
                                      cbHowToClaimUrl                                        = Some("/cb/cbHowToClaimUrl"),
                                      cbHowToClaimUrlCy                                      = Some("/cb/cbHowToClaimUrlCy"),
                                      cbFullTimeEducationUrl                                 = Some("/cb/cbFullTimeEducationUrl"),
                                      cbFullTimeEducationUrlCy                               = Some("/cb/cbFullTimeEducationUrlCy"),
                                      cbWhatChangesUrl                                       = Some("/cb/cbWhatChangesUrl"),
                                      cbWhatChangesUrlCy                                     = Some("/cb/cbWhatChangesUrlCy"),
                                      statePensionUrl                                        = Some("/statePensionUrl"),
                                      niSummaryUrl                                           = Some("/niSummaryUrl"),
                                      niContributionsUrl                                     = Some("/niContributionsUrl"),
                                      otherTaxesDigitalAssistantUrl                          = Some("/otherTaxesDigitalAssistantUrl"),
                                      otherTaxesDigitalAssistantUrlCy                        = Some("/otherTaxesDigitalAssistantUrlCy"),
                                      enableCustomerSatisfactionSurveys                      = false,
                                      findMyNinoAddToWallet                                  = false,
                                      disableYourEmploymentIncomeChart                       = true,
                                      disableYourEmploymentIncomeChartAndroid                = true,
                                      disableYourEmploymentIncomeChartIos                    = true,
                                      findMyNinoAddToGoogleWallet                            = false)

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                                         = false),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled                 = false),
        FeatureFlag("paperlessAlertDialogs", enabled                                   = false),
        FeatureFlag("paperlessAdverts", enabled                                        = false),
        FeatureFlag("htsAdverts", enabled                                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                                    = false),
        FeatureFlag("customerSatisfactionSurveys", enabled                             = false),
        FeatureFlag("findMyNinoAddToWallet", enabled                                   = false),
        FeatureFlag("disableYourEmploymentIncomeChart", enabled                        = true),
        FeatureFlag("disableYourEmploymentIncomeChartAndroid", enabled                 = true),
        FeatureFlag("disableYourEmploymentIncomeChartIos", enabled                     = true),
        FeatureFlag("findMyNinoAddToGoogleWallet", enabled                             = false)
      )
      (result \ messages).toOption.value shouldBe messagesSuccessResponse
      (result \ user).toOption.value     shouldBe userExpectedResponse
      (result \ "urls").get
        .as[List[URL]] shouldBe List(
        URL("cbProofOfEntitlementUrl", "/cb/cbProofOfEntitlementUrl"),
        URL("cbProofOfEntitlementUrlCy", "/cb/cbProofOfEntitlementUrlCy"),
        URL("cbPaymentHistoryUrl", "/cb/cbPaymentHistoryUrl"),
        URL("cbPaymentHistoryUrlCy", "/cb/cbPaymentHistoryUrlCy"),
        URL("cbChangeBankAccountUrl", "/cb/cbChangeBankAccountUrl"),
        URL("cbChangeBankAccountUrlCy", "/cb/cbChangeBankAccountUrlCy"),
        URL("cbHomeUrl", "/cb/cbHomeUrl"),
        URL("cbHomeUrlCy", "/cb/cbHomeUrlCy"),
        URL("cbHowToClaimUrl", "/cb/cbHowToClaimUrl"),
        URL("cbHowToClaimUrlCy", "/cb/cbHowToClaimUrlCy"),
        URL("cbFullTimeEducationUrl", "/cb/cbFullTimeEducationUrl"),
        URL("cbFullTimeEducationUrlCy", "/cb/cbFullTimeEducationUrlCy"),
        URL("cbWhatChangesUrl", "/cb/cbWhatChangesUrl"),
        URL("cbWhatChangesUrlCy", "/cb/cbWhatChangesUrlCy"),
        URL("statePensionUrl", "/statePensionUrl"),
        URL("niSummaryUrl", "/niSummaryUrl"),
        URL("niContributionsUrl", "/niContributionsUrl"),
        URL("otherTaxesDigitalAssistantUrl", "/otherTaxesDigitalAssistantUrl"),
        URL("otherTaxesDigitalAssistantUrlCy", "/otherTaxesDigitalAssistantUrlCy")
      )
    }
  }

  "a response" should {
    "not contain an entry for help-to-save when the hts call fails" in {
      val sut =
        new StartupServiceImpl[TestF](dummyConnector(htsResponse = new Exception("hts failed").error),
                                      false,
                                      enablePushNotificationTokenRegistration                = false,
                                      enablePaperlessAlertDialogs                            = false,
                                      enablePaperlessAdverts                                 = false,
                                      enableHtsAdverts                                       = false,
                                      enableAnnualTaxSummaryLink                             = false,
                                      cbProofOfEntitlementUrl                                = Some("/cb/cbProofOfEntitlementUrl"),
                                      cbProofOfEntitlementUrlCy                              = Some("/cb/cbProofOfEntitlementUrlCy"),
                                      cbPaymentHistoryUrl                                    = Some("/cb/cbPaymentHistoryUrl"),
                                      cbPaymentHistoryUrlCy                                  = Some("/cb/cbPaymentHistoryUrlCy"),
                                      cbChangeBankAccountUrl                                 = Some("/cb/cbChangeBankAccountUrl"),
                                      cbChangeBankAccountUrlCy                               = Some("/cb/cbChangeBankAccountUrlCy"),
                                      cbHomeUrl                                              = Some("/cb/cbHomeUrl"),
                                      cbHomeUrlCy                                            = Some("/cb/cbHomeUrlCy"),
                                      cbHowToClaimUrl                                        = Some("/cb/cbHowToClaimUrl"),
                                      cbHowToClaimUrlCy                                      = Some("/cb/cbHowToClaimUrlCy"),
                                      cbFullTimeEducationUrl                                 = Some("/cb/cbFullTimeEducationUrl"),
                                      cbFullTimeEducationUrlCy                               = Some("/cb/cbFullTimeEducationUrlCy"),
                                      cbWhatChangesUrl                                       = Some("/cb/cbWhatChangesUrl"),
                                      cbWhatChangesUrlCy                                     = Some("/cb/cbWhatChangesUrlCy"),
                                      statePensionUrl                                        = Some("/statePensionUrl"),
                                      niSummaryUrl                                           = Some("/niSummaryUrl"),
                                      niContributionsUrl                                     = Some("/niContributionsUrl"),
                                      otherTaxesDigitalAssistantUrl                          = Some("/otherTaxesDigitalAssistantUrl"),
                                      otherTaxesDigitalAssistantUrlCy                        = Some("/otherTaxesDigitalAssistantUrlCy"),
                                      enableCustomerSatisfactionSurveys                      = false,
                                      findMyNinoAddToWallet                                  = false,
                                      disableYourEmploymentIncomeChart                       = true,
                                      disableYourEmploymentIncomeChartAndroid                = true,
                                      disableYourEmploymentIncomeChartIos                    = true,
                                      findMyNinoAddToGoogleWallet                            = false)

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption               shouldBe None
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                                         = false),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled                 = false),
        FeatureFlag("paperlessAlertDialogs", enabled                                   = false),
        FeatureFlag("paperlessAdverts", enabled                                        = false),
        FeatureFlag("htsAdverts", enabled                                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                                    = false),
        FeatureFlag("customerSatisfactionSurveys", enabled                             = false),
        FeatureFlag("findMyNinoAddToWallet", enabled                                   = false),
        FeatureFlag("disableYourEmploymentIncomeChart", enabled                        = true),
        FeatureFlag("disableYourEmploymentIncomeChartAndroid", enabled                 = true),
        FeatureFlag("disableYourEmploymentIncomeChartIos", enabled                     = true),
        FeatureFlag("findMyNinoAddToGoogleWallet", enabled                             = false)
      )
      (result \ messages).toOption.value shouldBe messagesSuccessResponse
      (result \ user).toOption.value     shouldBe userExpectedResponse
      (result \ "urls").get
        .as[List[URL]] shouldBe List(
        URL("cbProofOfEntitlementUrl", "/cb/cbProofOfEntitlementUrl"),
        URL("cbProofOfEntitlementUrlCy", "/cb/cbProofOfEntitlementUrlCy"),
        URL("cbPaymentHistoryUrl", "/cb/cbPaymentHistoryUrl"),
        URL("cbPaymentHistoryUrlCy", "/cb/cbPaymentHistoryUrlCy"),
        URL("cbChangeBankAccountUrl", "/cb/cbChangeBankAccountUrl"),
        URL("cbChangeBankAccountUrlCy", "/cb/cbChangeBankAccountUrlCy"),
        URL("cbHomeUrl", "/cb/cbHomeUrl"),
        URL("cbHomeUrlCy", "/cb/cbHomeUrlCy"),
        URL("cbHowToClaimUrl", "/cb/cbHowToClaimUrl"),
        URL("cbHowToClaimUrlCy", "/cb/cbHowToClaimUrlCy"),
        URL("cbFullTimeEducationUrl", "/cb/cbFullTimeEducationUrl"),
        URL("cbFullTimeEducationUrlCy", "/cb/cbFullTimeEducationUrlCy"),
        URL("cbWhatChangesUrl", "/cb/cbWhatChangesUrl"),
        URL("cbWhatChangesUrlCy", "/cb/cbWhatChangesUrlCy"),
        URL("statePensionUrl", "/statePensionUrl"),
        URL("niSummaryUrl", "/niSummaryUrl"),
        URL("niContributionsUrl", "/niContributionsUrl"),
        URL("otherTaxesDigitalAssistantUrl", "/otherTaxesDigitalAssistantUrl"),
        URL("otherTaxesDigitalAssistantUrlCy", "/otherTaxesDigitalAssistantUrlCy")
      )
    }

    "contain an error entry for tcr when the tcr call fails" in {
      val sut =
        new StartupServiceImpl[TestF](dummyConnector(tcrResponse = new Exception("tcr failed").error),
                                      false,
                                      enablePushNotificationTokenRegistration                = false,
                                      enablePaperlessAlertDialogs                            = false,
                                      enablePaperlessAdverts                                 = false,
                                      enableHtsAdverts                                       = false,
                                      enableAnnualTaxSummaryLink                             = false,
                                      cbProofOfEntitlementUrl                                = Some("/cb/cbProofOfEntitlementUrl"),
                                      cbProofOfEntitlementUrlCy                              = Some("/cb/cbProofOfEntitlementUrlCy"),
                                      cbPaymentHistoryUrl                                    = Some("/cb/cbPaymentHistoryUrl"),
                                      cbPaymentHistoryUrlCy                                  = Some("/cb/cbPaymentHistoryUrlCy"),
                                      cbChangeBankAccountUrl                                 = Some("/cb/cbChangeBankAccountUrl"),
                                      cbChangeBankAccountUrlCy                               = Some("/cb/cbChangeBankAccountUrlCy"),
                                      cbHomeUrl                                              = Some("/cb/cbHomeUrl"),
                                      cbHomeUrlCy                                            = Some("/cb/cbHomeUrlCy"),
                                      cbHowToClaimUrl                                        = Some("/cb/cbHowToClaimUrl"),
                                      cbHowToClaimUrlCy                                      = Some("/cb/cbHowToClaimUrlCy"),
                                      cbFullTimeEducationUrl                                 = Some("/cb/cbFullTimeEducationUrl"),
                                      cbFullTimeEducationUrlCy                               = Some("/cb/cbFullTimeEducationUrlCy"),
                                      cbWhatChangesUrl                                       = Some("/cb/cbWhatChangesUrl"),
                                      cbWhatChangesUrlCy                                     = Some("/cb/cbWhatChangesUrlCy"),
                                      statePensionUrl                                        = Some("/statePensionUrl"),
                                      niSummaryUrl                                           = Some("/niSummaryUrl"),
                                      niContributionsUrl                                     = Some("/niContributionsUrl"),
                                      otherTaxesDigitalAssistantUrl                          = Some("/otherTaxesDigitalAssistantUrl"),
                                      otherTaxesDigitalAssistantUrlCy                        = Some("/otherTaxesDigitalAssistantUrlCy"),
                                      enableCustomerSatisfactionSurveys                      = false,
                                      findMyNinoAddToWallet                                  = false,
                                      disableYourEmploymentIncomeChart                       = true,
                                      disableYourEmploymentIncomeChartAndroid                = true,
                                      disableYourEmploymentIncomeChartIos                    = true,
                                      findMyNinoAddToGoogleWallet                            = false)

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe obj("submissionsState" -> "error")
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                                         = false),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled                 = false),
        FeatureFlag("paperlessAlertDialogs", enabled                                   = false),
        FeatureFlag("paperlessAdverts", enabled                                        = false),
        FeatureFlag("htsAdverts", enabled                                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                                    = false),
        FeatureFlag("customerSatisfactionSurveys", enabled                             = false),
        FeatureFlag("findMyNinoAddToWallet", enabled                                   = false),
        FeatureFlag("disableYourEmploymentIncomeChart", enabled                        = true),
        FeatureFlag("disableYourEmploymentIncomeChartAndroid", enabled                 = true),
        FeatureFlag("disableYourEmploymentIncomeChartIos", enabled                     = true),
        FeatureFlag("findMyNinoAddToGoogleWallet", enabled                             = false)
      )
      (result \ messages).toOption.value shouldBe messagesSuccessResponse
      (result \ user).toOption.value     shouldBe userExpectedResponse
      (result \ "urls").get
        .as[List[URL]] shouldBe List(
        URL("cbProofOfEntitlementUrl", "/cb/cbProofOfEntitlementUrl"),
        URL("cbProofOfEntitlementUrlCy", "/cb/cbProofOfEntitlementUrlCy"),
        URL("cbPaymentHistoryUrl", "/cb/cbPaymentHistoryUrl"),
        URL("cbPaymentHistoryUrlCy", "/cb/cbPaymentHistoryUrlCy"),
        URL("cbChangeBankAccountUrl", "/cb/cbChangeBankAccountUrl"),
        URL("cbChangeBankAccountUrlCy", "/cb/cbChangeBankAccountUrlCy"),
        URL("cbHomeUrl", "/cb/cbHomeUrl"),
        URL("cbHomeUrlCy", "/cb/cbHomeUrlCy"),
        URL("cbHowToClaimUrl", "/cb/cbHowToClaimUrl"),
        URL("cbHowToClaimUrlCy", "/cb/cbHowToClaimUrlCy"),
        URL("cbFullTimeEducationUrl", "/cb/cbFullTimeEducationUrl"),
        URL("cbFullTimeEducationUrlCy", "/cb/cbFullTimeEducationUrlCy"),
        URL("cbWhatChangesUrl", "/cb/cbWhatChangesUrl"),
        URL("cbWhatChangesUrlCy", "/cb/cbWhatChangesUrlCy"),
        URL("statePensionUrl", "/statePensionUrl"),
        URL("niSummaryUrl", "/niSummaryUrl"),
        URL("niContributionsUrl", "/niContributionsUrl"),
        URL("otherTaxesDigitalAssistantUrl", "/otherTaxesDigitalAssistantUrl"),
        URL("otherTaxesDigitalAssistantUrlCy", "/otherTaxesDigitalAssistantUrlCy")
      )
    }

    "contain an empty lists entry for messages when the messages call fails" in {
      val sut = new StartupServiceImpl[TestF](
        dummyConnector(inAppMessagesResponse = new Exception("message call failed").error),
        false,
        enablePushNotificationTokenRegistration                = false,
        enablePaperlessAlertDialogs                            = false,
        enablePaperlessAdverts                                 = false,
        enableHtsAdverts                                       = false,
        enableAnnualTaxSummaryLink                             = false,
        cbProofOfEntitlementUrl                                = Some("/cb/cbProofOfEntitlementUrl"),
        cbProofOfEntitlementUrlCy                              = Some("/cb/cbProofOfEntitlementUrlCy"),
        cbPaymentHistoryUrl                                    = Some("/cb/cbPaymentHistoryUrl"),
        cbPaymentHistoryUrlCy                                  = Some("/cb/cbPaymentHistoryUrlCy"),
        cbChangeBankAccountUrl                                 = Some("/cb/cbChangeBankAccountUrl"),
        cbChangeBankAccountUrlCy                               = Some("/cb/cbChangeBankAccountUrlCy"),
        cbHomeUrl                                              = Some("/cb/cbHomeUrl"),
        cbHomeUrlCy                                            = Some("/cb/cbHomeUrlCy"),
        cbHowToClaimUrl                                        = Some("/cb/cbHowToClaimUrl"),
        cbHowToClaimUrlCy                                      = Some("/cb/cbHowToClaimUrlCy"),
        cbFullTimeEducationUrl                                 = Some("/cb/cbFullTimeEducationUrl"),
        cbFullTimeEducationUrlCy                               = Some("/cb/cbFullTimeEducationUrlCy"),
        cbWhatChangesUrl                                       = Some("/cb/cbWhatChangesUrl"),
        cbWhatChangesUrlCy                                     = Some("/cb/cbWhatChangesUrlCy"),
        statePensionUrl                                        = Some("/statePensionUrl"),
        niSummaryUrl                                           = Some("/niSummaryUrl"),
        niContributionsUrl                                     = Some("/niContributionsUrl"),
        otherTaxesDigitalAssistantUrl                          = Some("/otherTaxesDigitalAssistantUrl"),
        otherTaxesDigitalAssistantUrlCy                        = Some("/otherTaxesDigitalAssistantUrlCy"),
        enableCustomerSatisfactionSurveys                      = false,
        findMyNinoAddToWallet                                  = false,
        disableYourEmploymentIncomeChart                       = true,
        disableYourEmploymentIncomeChartAndroid                = true,
        disableYourEmploymentIncomeChartIos                    = true,
        findMyNinoAddToGoogleWallet                            = false
      )

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe obj("submissionsState" -> "open")
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                                         = false),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled                 = false),
        FeatureFlag("paperlessAlertDialogs", enabled                                   = false),
        FeatureFlag("paperlessAdverts", enabled                                        = false),
        FeatureFlag("htsAdverts", enabled                                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                                    = false),
        FeatureFlag("customerSatisfactionSurveys", enabled                             = false),
        FeatureFlag("findMyNinoAddToWallet", enabled                                   = false),
        FeatureFlag("disableYourEmploymentIncomeChart", enabled                        = true),
        FeatureFlag("disableYourEmploymentIncomeChartAndroid", enabled                 = true),
        FeatureFlag("disableYourEmploymentIncomeChartIos", enabled                     = true),
        FeatureFlag("findMyNinoAddToGoogleWallet", enabled                             = false)
      )
      (result \ messages).toOption.value shouldBe Json.parse("""{
                                                               |  "paye": [],
                                                               |  "tc": [],
                                                               |  "hts": [],
                                                               |  "tcp": []
                                                               |}
                                                               |""".stripMargin)
      (result \ user).toOption.value     shouldBe userExpectedResponse
      (result \ "urls").get
        .as[List[URL]] shouldBe List(
        URL("cbProofOfEntitlementUrl", "/cb/cbProofOfEntitlementUrl"),
        URL("cbProofOfEntitlementUrlCy", "/cb/cbProofOfEntitlementUrlCy"),
        URL("cbPaymentHistoryUrl", "/cb/cbPaymentHistoryUrl"),
        URL("cbPaymentHistoryUrlCy", "/cb/cbPaymentHistoryUrlCy"),
        URL("cbChangeBankAccountUrl", "/cb/cbChangeBankAccountUrl"),
        URL("cbChangeBankAccountUrlCy", "/cb/cbChangeBankAccountUrlCy"),
        URL("cbHomeUrl", "/cb/cbHomeUrl"),
        URL("cbHomeUrlCy", "/cb/cbHomeUrlCy"),
        URL("cbHowToClaimUrl", "/cb/cbHowToClaimUrl"),
        URL("cbHowToClaimUrlCy", "/cb/cbHowToClaimUrlCy"),
        URL("cbFullTimeEducationUrl", "/cb/cbFullTimeEducationUrl"),
        URL("cbFullTimeEducationUrlCy", "/cb/cbFullTimeEducationUrlCy"),
        URL("cbWhatChangesUrl", "/cb/cbWhatChangesUrl"),
        URL("cbWhatChangesUrlCy", "/cb/cbWhatChangesUrlCy"),
        URL("statePensionUrl", "/statePensionUrl"),
        URL("niSummaryUrl", "/niSummaryUrl"),
        URL("niContributionsUrl", "/niContributionsUrl"),
        URL("otherTaxesDigitalAssistantUrl", "/otherTaxesDigitalAssistantUrl"),
        URL("otherTaxesDigitalAssistantUrlCy", "/otherTaxesDigitalAssistantUrlCy")
      )
    }

    "not contain an entry for user when the citizen details call fails" in {
      val sut =
        new StartupServiceImpl[TestF](dummyConnector(citizenDetailsResponse = new Exception("cid failed").error),
                                      false,
                                      enablePushNotificationTokenRegistration                = false,
                                      enablePaperlessAlertDialogs                            = false,
                                      enablePaperlessAdverts                                 = false,
                                      enableHtsAdverts                                       = false,
                                      enableAnnualTaxSummaryLink                             = false,
                                      cbProofOfEntitlementUrl                                = Some("/cb/cbProofOfEntitlementUrl"),
                                      cbProofOfEntitlementUrlCy                              = Some("/cb/cbProofOfEntitlementUrlCy"),
                                      cbPaymentHistoryUrl                                    = Some("/cb/cbPaymentHistoryUrl"),
                                      cbPaymentHistoryUrlCy                                  = Some("/cb/cbPaymentHistoryUrlCy"),
                                      cbChangeBankAccountUrl                                 = Some("/cb/cbChangeBankAccountUrl"),
                                      cbChangeBankAccountUrlCy                               = Some("/cb/cbChangeBankAccountUrlCy"),
                                      cbHomeUrl                                              = Some("/cb/cbHomeUrl"),
                                      cbHomeUrlCy                                            = Some("/cb/cbHomeUrlCy"),
                                      cbHowToClaimUrl                                        = Some("/cb/cbHowToClaimUrl"),
                                      cbHowToClaimUrlCy                                      = Some("/cb/cbHowToClaimUrlCy"),
                                      cbFullTimeEducationUrl                                 = Some("/cb/cbFullTimeEducationUrl"),
                                      cbFullTimeEducationUrlCy                               = Some("/cb/cbFullTimeEducationUrlCy"),
                                      cbWhatChangesUrl                                       = Some("/cb/cbWhatChangesUrl"),
                                      cbWhatChangesUrlCy                                     = Some("/cb/cbWhatChangesUrlCy"),
                                      statePensionUrl                                        = Some("/statePensionUrl"),
                                      niSummaryUrl                                           = Some("/niSummaryUrl"),
                                      niContributionsUrl                                     = Some("/niContributionsUrl"),
                                      otherTaxesDigitalAssistantUrl                          = Some("/otherTaxesDigitalAssistantUrl"),
                                      otherTaxesDigitalAssistantUrlCy                        = Some("/otherTaxesDigitalAssistantUrlCy"),
                                      enableCustomerSatisfactionSurveys                      = false,
                                      findMyNinoAddToWallet                                  = false,
                                      disableYourEmploymentIncomeChart                       = true,
                                      disableYourEmploymentIncomeChartAndroid                = true,
                                      disableYourEmploymentIncomeChartIos                    = true,
                                      findMyNinoAddToGoogleWallet                            = false)

      val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ "feature").get
        .as[List[FeatureFlag]] shouldBe List(
        FeatureFlag("userPanelSignUp", enabled                                         = false),
        FeatureFlag("enablePushNotificationTokenRegistration", enabled                 = false),
        FeatureFlag("paperlessAlertDialogs", enabled                                   = false),
        FeatureFlag("paperlessAdverts", enabled                                        = false),
        FeatureFlag("htsAdverts", enabled                                              = false),
        FeatureFlag("annualTaxSummaryLink", enabled                                    = false),
        FeatureFlag("customerSatisfactionSurveys", enabled                             = false),
        FeatureFlag("findMyNinoAddToWallet", enabled                                   = false),
        FeatureFlag("disableYourEmploymentIncomeChart", enabled                        = true),
        FeatureFlag("disableYourEmploymentIncomeChartAndroid", enabled                 = true),
        FeatureFlag("disableYourEmploymentIncomeChartIos", enabled                     = true),
        FeatureFlag("findMyNinoAddToGoogleWallet", enabled                             = false)
      )
      (result \ messages).toOption.value shouldBe messagesSuccessResponse
      (result \ user).toOption           shouldBe None
      (result \ "urls").get
        .as[List[URL]] shouldBe List(
        URL("cbProofOfEntitlementUrl", "/cb/cbProofOfEntitlementUrl"),
        URL("cbProofOfEntitlementUrlCy", "/cb/cbProofOfEntitlementUrlCy"),
        URL("cbPaymentHistoryUrl", "/cb/cbPaymentHistoryUrl"),
        URL("cbPaymentHistoryUrlCy", "/cb/cbPaymentHistoryUrlCy"),
        URL("cbChangeBankAccountUrl", "/cb/cbChangeBankAccountUrl"),
        URL("cbChangeBankAccountUrlCy", "/cb/cbChangeBankAccountUrlCy"),
        URL("cbHomeUrl", "/cb/cbHomeUrl"),
        URL("cbHomeUrlCy", "/cb/cbHomeUrlCy"),
        URL("cbHowToClaimUrl", "/cb/cbHowToClaimUrl"),
        URL("cbHowToClaimUrlCy", "/cb/cbHowToClaimUrlCy"),
        URL("cbFullTimeEducationUrl", "/cb/cbFullTimeEducationUrl"),
        URL("cbFullTimeEducationUrlCy", "/cb/cbFullTimeEducationUrlCy"),
        URL("cbWhatChangesUrl", "/cb/cbWhatChangesUrl"),
        URL("cbWhatChangesUrlCy", "/cb/cbWhatChangesUrlCy"),
        URL("statePensionUrl", "/statePensionUrl"),
        URL("niSummaryUrl", "/niSummaryUrl"),
        URL("niContributionsUrl", "/niContributionsUrl"),
        URL("otherTaxesDigitalAssistantUrl", "/otherTaxesDigitalAssistantUrl"),
        URL("otherTaxesDigitalAssistantUrlCy", "/otherTaxesDigitalAssistantUrlCy")
      )
    }
  }

  "not contain an entry for URLs that have no value" in {
    val sut =
      new StartupServiceImpl[TestF](dummyConnector(citizenDetailsResponse = new Exception("cid failed").error),
                                    false,
                                    enablePushNotificationTokenRegistration                = false,
                                    enablePaperlessAlertDialogs                            = false,
                                    enablePaperlessAdverts                                 = false,
                                    enableHtsAdverts                                       = false,
                                    enableAnnualTaxSummaryLink                             = false,
                                    cbProofOfEntitlementUrl                                = Some("/cb/cbProofOfEntitlementUrl"),
                                    cbProofOfEntitlementUrlCy                              = None,
                                    cbPaymentHistoryUrl                                    = Some("/cb/cbPaymentHistoryUrl"),
                                    cbPaymentHistoryUrlCy                                  = None,
                                    cbChangeBankAccountUrl                                 = Some("/cb/cbChangeBankAccountUrl"),
                                    cbChangeBankAccountUrlCy                               = None,
                                    cbHomeUrl                                              = Some("/cb/cbHomeUrl"),
                                    cbHomeUrlCy                                            = None,
                                    cbHowToClaimUrl                                        = Some("/cb/cbHowToClaimUrl"),
                                    cbHowToClaimUrlCy                                      = None,
                                    cbFullTimeEducationUrl                                 = Some("/cb/cbFullTimeEducationUrl"),
                                    cbFullTimeEducationUrlCy                               = None,
                                    cbWhatChangesUrl                                       = Some("/cb/cbWhatChangesUrl"),
                                    cbWhatChangesUrlCy                                     = None,
                                    statePensionUrl                                        = Some("/statePensionUrl"),
                                    niSummaryUrl                                           = Some("/niSummaryUrl"),
                                    niContributionsUrl                                     = Some("/niContributionsUrl"),
                                    otherTaxesDigitalAssistantUrl                          = Some("/otherTaxesDigitalAssistantUrl"),
                                    otherTaxesDigitalAssistantUrlCy                        = Some("/otherTaxesDigitalAssistantUrlCy"),
                                    enableCustomerSatisfactionSurveys                      = false,
                                    findMyNinoAddToWallet                                  = false,
                                    disableYourEmploymentIncomeChart                       = true,
                                    disableYourEmploymentIncomeChartAndroid                = true,
                                    disableYourEmploymentIncomeChartIos                    = true,
                                    findMyNinoAddToGoogleWallet                            = false)

    val result: JsObject = sut.startup("nino", journeyId)(HeaderCarrier()).unsafeGet

    (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
    (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
    (result \ "feature").get
      .as[List[FeatureFlag]] shouldBe List(
      FeatureFlag("userPanelSignUp", enabled                                         = false),
      FeatureFlag("enablePushNotificationTokenRegistration", enabled                 = false),
      FeatureFlag("paperlessAlertDialogs", enabled                                   = false),
      FeatureFlag("paperlessAdverts", enabled                                        = false),
      FeatureFlag("htsAdverts", enabled                                              = false),
      FeatureFlag("annualTaxSummaryLink", enabled                                    = false),
      FeatureFlag("customerSatisfactionSurveys", enabled                             = false),
      FeatureFlag("findMyNinoAddToWallet", enabled                                   = false),
      FeatureFlag("disableYourEmploymentIncomeChart", enabled                        = true),
      FeatureFlag("disableYourEmploymentIncomeChartAndroid", enabled                 = true),
      FeatureFlag("disableYourEmploymentIncomeChartIos", enabled                     = true),
      FeatureFlag("findMyNinoAddToGoogleWallet", enabled                             = false)
    )
    (result \ messages).toOption.value shouldBe messagesSuccessResponse
    (result \ user).toOption           shouldBe None
    (result \ "urls").get
      .as[List[URL]] shouldBe List(
      URL("cbProofOfEntitlementUrl", "/cb/cbProofOfEntitlementUrl"),
      URL("cbPaymentHistoryUrl", "/cb/cbPaymentHistoryUrl"),
      URL("cbChangeBankAccountUrl", "/cb/cbChangeBankAccountUrl"),
      URL("cbHomeUrl", "/cb/cbHomeUrl"),
      URL("cbHowToClaimUrl", "/cb/cbHowToClaimUrl"),
      URL("cbFullTimeEducationUrl", "/cb/cbFullTimeEducationUrl"),
      URL("cbWhatChangesUrl", "/cb/cbWhatChangesUrl"),
      URL("statePensionUrl", "/statePensionUrl"),
      URL("niSummaryUrl", "/niSummaryUrl"),
      URL("niContributionsUrl", "/niContributionsUrl"),
      URL("otherTaxesDigitalAssistantUrl", "/otherTaxesDigitalAssistantUrl"),
      URL("otherTaxesDigitalAssistantUrlCy", "/otherTaxesDigitalAssistantUrlCy")
    )
  }
}
