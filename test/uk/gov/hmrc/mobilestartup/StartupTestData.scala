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

package uk.gov.hmrc.mobilestartup

import cats.implicits._
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.libs.json.Json.obj
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.shuttering.Shuttering
import uk.gov.hmrc.mobilestartup.model.{CidPerson, EnrolmentStoreResponse}
import uk.gov.hmrc.mobilestartup.services.{FeatureFlag, StartupServiceImpl, URL}

trait StartupTestData extends TestF {

  val helpToSave         = "helpToSave"
  val taxCreditsRenewals = "taxCreditRenewals"
  val messages           = "messages"
  val user               = "user"
  val successfulResponse = JsString("success")

  val htsSuccessResponse:      JsValue = successfulResponse
  val tcrSuccessResponse:      JsValue = obj("submissionsState" -> "open")
  val messagesSuccessResponse: JsValue = Json.parse("""{
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

  val citizenDetailsSuccessResponse: JsValue = Json.parse("""{
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

  val userExpectedResponse: JsValue = Json.parse("""{
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

  val startupService =
    new StartupServiceImpl[TestF](
      dummyConnector(),
      userPanelSignUp                           = false,
      enablePushNotificationTokenRegistration   = false,
      enablePaperlessAlertDialogs               = false,
      enablePaperlessAdverts                    = false,
      enableHtsAdverts                          = false,
      enableAnnualTaxSummaryLink                = false,
      cbProofOfEntitlementUrl                   = Some("/cb/cbProofOfEntitlementUrl"),
      cbProofOfEntitlementUrlCy                 = Some("/cb/cbProofOfEntitlementUrlCy"),
      cbPaymentHistoryUrl                       = Some("/cb/cbPaymentHistoryUrl"),
      cbPaymentHistoryUrlCy                     = Some("/cb/cbPaymentHistoryUrlCy"),
      cbChangeBankAccountUrl                    = Some("/cb/cbChangeBankAccountUrl"),
      cbChangeBankAccountUrlCy                  = Some("/cb/cbChangeBankAccountUrlCy"),
      cbHomeUrl                                 = Some("/cb/cbHomeUrl"),
      cbHomeUrlCy                               = Some("/cb/cbHomeUrlCy"),
      cbHowToClaimUrl                           = Some("/cb/cbHowToClaimUrl"),
      cbHowToClaimUrlCy                         = Some("/cb/cbHowToClaimUrlCy"),
      cbFullTimeEducationUrl                    = Some("/cb/cbFullTimeEducationUrl"),
      cbFullTimeEducationUrlCy                  = Some("/cb/cbFullTimeEducationUrlCy"),
      cbWhatChangesUrl                          = Some("/cb/cbWhatChangesUrl"),
      cbWhatChangesUrlCy                        = Some("/cb/cbWhatChangesUrlCy"),
      statePensionUrl                           = Some("/statePensionUrl"),
      niSummaryUrl                              = Some("/niSummaryUrl"),
      niContributionsUrl                        = Some("/niContributionsUrl"),
      otherTaxesDigitalAssistantUrl             = Some("/otherTaxesDigitalAssistantUrl"),
      otherTaxesDigitalAssistantUrlCy           = Some("/otherTaxesDigitalAssistantUrlCy"),
      payeDigitalAssistantUrl                   = Some("/payeDigitalAssistantUrl"),
      payeDigitalAssistantUrlCy                 = Some("/payeDigitalAssistantUrlCy"),
      incomeTaxGeneralEnquiriesUrl              = Some("/incomeTaxGeneralEnquiriesUrl"),
      incomeTaxGeneralEnquiriesUrlCy            = Some("/incomeTaxGeneralEnquiriesUrlCy"),
      learnAboutCallChargesUrl                  = Some("/learnAboutCallChargesUrl"),
      learnAboutCallChargesUrlCy                = Some("/learnAboutCallChargesUrlCy"),
      statePensionAgeUrl                        = Some("/statePensionAgeUrl"),
      tcNationalInsuranceRatesLettersUrl        = Some("/tcNationalInsuranceRatesLettersUrl"),
      tcNationalInsuranceRatesLettersUrlCy      = Some("/tcNationalInsuranceRatesLettersUrlCy"),
      tcPersonalAllowanceUrl                    = Some("/tcPersonalAllowanceUrl"),
      tcPersonalAllowanceUrlCy                  = Some("/tcPersonalAllowanceUrlCy"),
      scottishIncomeTaxUrl                      = Some("/scottishIncomeTaxUrl"),
      scottishIncomeTaxUrlCy                    = Some("/scottishIncomeTaxUrlCy"),
      cbTaxChargeUrl                            = Some("/cbTaxChargeUrl"),
      cbTaxChargeUrlCy                          = Some("/cbTaxChargeUrlCy"),
      selfAssessmentHelpAppealingPenaltiesUrl   = Some("/selfAssessmentHelpAppealingPenaltiesUrl"),
      selfAssessmentHelpAppealingPenaltiesUrlCy = Some("/selfAssessmentHelpAppealingPenaltiesUrlCy"),
      addMissingTaxableIncomeUrl                = Some("/addMissingTaxableIncomeUrl"),
      helpToSaveGeneralEnquiriesUrl             = Some("/helpToSaveGeneralEnquiriesUrl"),
      helpToSaveGeneralEnquiriesUrlCy           = Some("/helpToSaveGeneralEnquiriesUrlCy"),
      helpToSaveDigitalAssistantUrl             = Some("/helpToSaveDigitalAssistantUrl"),
      selfAssessmentGeneralEnquiriesUrl         = Some("/selfAssessmentGeneralEnquiriesUrl"),
      selfAssessmentGeneralEnquiriesUrlCy       = Some("/selfAssessmentGeneralEnquiriesUrlCy"),
      simpleAssessmentGeneralEnquiriesUrl       = Some("/simpleAssessmentGeneralEnquiriesUrl"),
      simpleAssessmentGeneralEnquiriesUrlCy     = Some("/simpleAssessmentGeneralEnquiriesUrlCy"),
      cbGeneralEnquiriesUrl                     = Some("/cbGeneralEnquiriesUrl"),
      cbGeneralEnquiriesUrlCy                   = Some("/cbGeneralEnquiriesUrlCy"),
      taxCreditsGeneralEnquiriesUrl             = Some("/taxCreditsGeneralEnquiriesUrl"),
      taxCreditsGeneralEnquiriesUrlCy           = Some("/taxCreditsGeneralEnquiriesUrlCy"),
      otherTaxesGeneralEnquiriesUrl             = Some("/otherTaxesGeneralEnquiriesUrl"),
      otherTaxesGeneralEnquiriesUrlCy           = Some("/otherTaxesGeneralEnquiriesUrlCy"),
      findRepaymentPlanUrl                      = Some("/findRepaymentPlanUrl"),
      findRepaymentPlanUrlCy                    = Some("/findRepaymentPlanUrlCy"),
      pensionAnnualAllowanceUrl                 = Some("/pensionAnnualAllowanceUrl"),
      pensionAnnualAllowanceUrlCy               = Some("/pensionAnnualAllowanceUrlCy"),
      childBenefitDigitalAssistantUrl           = Some("/childBenefitDigitalAssistantUrl"),
      childBenefitDigitalAssistantUrlCy         = Some("/childBenefitDigitalAssistantUrlCy"),
      incomeTaxDigitalAssistantUrl              = Some("/incomeTaxDigitalAssistantUrl"),
      incomeTaxDigitalAssistantUrlCy            = Some("/incomeTaxDigitalAssistantUrlCy"),
      selfAssessmentDigitalAssistantUrl         = Some("/selfAssessmentDigitalAssistantUrl"),
      selfAssessmentDigitalAssistantUrlCy       = Some("/selfAssessmentDigitalAssistantUrlCy"),
      taxCreditsDigitalAssistantUrl             = Some("/taxCreditsDigitalAssistantUrl"),
      taxCreditsDigitalAssistantUrlCy           = Some("/taxCreditsDigitalAssistantUrlCy"),
      tcStateBenefitsUrl                        = Some("/tcStateBenefitsUrl"),
      tcStateBenefitsUrlCy                      = Some("/tcStateBenefitsUrlCy"),
      tcCompanyBenefitsUrl                      = Some("/tcCompanyBenefitsUrl"),
      tcCompanyBenefitsUrlCy                    = Some("/tcCompanyBenefitsUrlCy"),
      niAppleWalletUrl                          = Some("/niAppleWalletUrl"),
      niGoogleWalletUrl                         = Some("/niGoogleWalletUrl"),
      enableCustomerSatisfactionSurveys         = false,
      findMyNinoAddToWallet                     = false,
      disableYourEmploymentIncomeChart          = true,
      disableYourEmploymentIncomeChartAndroid   = true,
      disableYourEmploymentIncomeChartIos       = true,
      findMyNinoAddToGoogleWallet               = false,
      disableOldTaxCalculator                   = true
    )

  val expectedFeatureFlags = List(
    FeatureFlag("userPanelSignUp", enabled                         = false),
    FeatureFlag("enablePushNotificationTokenRegistration", enabled = false),
    FeatureFlag("paperlessAlertDialogs", enabled                   = false),
    FeatureFlag("paperlessAdverts", enabled                        = false),
    FeatureFlag("htsAdverts", enabled                              = false),
    FeatureFlag("annualTaxSummaryLink", enabled                    = false),
    FeatureFlag("customerSatisfactionSurveys", enabled             = false),
    FeatureFlag("findMyNinoAddToWallet", enabled                   = false),
    FeatureFlag("disableYourEmploymentIncomeChart", enabled        = true),
    FeatureFlag("disableYourEmploymentIncomeChartAndroid", enabled = true),
    FeatureFlag("disableYourEmploymentIncomeChartIos", enabled     = true),
    FeatureFlag("findMyNinoAddToGoogleWallet", enabled             = false),
    FeatureFlag("disableOldTaxCalculator", enabled                 = true),
    FeatureFlag("annualTaxSummaryLink", enabled                    = false)
  )

  val expectedURLs = List(
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
    URL("otherTaxesDigitalAssistantUrlCy", "/otherTaxesDigitalAssistantUrlCy"),
    URL("payeDigitalAssistantUrl", "/payeDigitalAssistantUrl"),
    URL("payeDigitalAssistantUrlCy", "/payeDigitalAssistantUrlCy"),
    URL("incomeTaxGeneralEnquiriesUrl", "/incomeTaxGeneralEnquiriesUrl"),
    URL("incomeTaxGeneralEnquiriesUrlCy", "/incomeTaxGeneralEnquiriesUrlCy"),
    URL("learnAboutCallChargesUrl", "/learnAboutCallChargesUrl"),
    URL("learnAboutCallChargesUrlCy", "/learnAboutCallChargesUrlCy"),
    URL("statePensionAgeUrl", "/statePensionAgeUrl"),
    URL("tcNationalInsuranceRatesLettersUrl", "/tcNationalInsuranceRatesLettersUrl"),
    URL("tcNationalInsuranceRatesLettersUrlCy", "/tcNationalInsuranceRatesLettersUrlCy"),
    URL("tcPersonalAllowanceUrl", "/tcPersonalAllowanceUrl"),
    URL("tcPersonalAllowanceUrlCy", "/tcPersonalAllowanceUrlCy"),
    URL("scottishIncomeTaxUrl", "/scottishIncomeTaxUrl"),
    URL("scottishIncomeTaxUrlCy", "/scottishIncomeTaxUrlCy"),
    URL("cbTaxChargeUrl", "/cbTaxChargeUrl"),
    URL("cbTaxChargeUrlCy", "/cbTaxChargeUrlCy"),
    URL("selfAssessmentHelpAppealingPenaltiesUrl", "/selfAssessmentHelpAppealingPenaltiesUrl"),
    URL("selfAssessmentHelpAppealingPenaltiesUrlCy", "/selfAssessmentHelpAppealingPenaltiesUrlCy"),
    URL("addMissingTaxableIncomeUrl", "/addMissingTaxableIncomeUrl"),
    URL("helpToSaveGeneralEnquiriesUrl", "/helpToSaveGeneralEnquiriesUrl"),
    URL("helpToSaveGeneralEnquiriesUrlCy", "/helpToSaveGeneralEnquiriesUrlCy"),
    URL("helpToSaveDigitalAssistantUrl", "/helpToSaveDigitalAssistantUrl"),
    URL("selfAssessmentGeneralEnquiriesUrl", "/selfAssessmentGeneralEnquiriesUrl"),
    URL("selfAssessmentGeneralEnquiriesUrlCy", "/selfAssessmentGeneralEnquiriesUrlCy"),
    URL("simpleAssessmentGeneralEnquiriesUrl", "/simpleAssessmentGeneralEnquiriesUrl"),
    URL("simpleAssessmentGeneralEnquiriesUrlCy", "/simpleAssessmentGeneralEnquiriesUrlCy"),
    URL("cbGeneralEnquiriesUrl", "/cbGeneralEnquiriesUrl"),
    URL("cbGeneralEnquiriesUrlCy", "/cbGeneralEnquiriesUrlCy"),
    URL("taxCreditsGeneralEnquiriesUrl", "/taxCreditsGeneralEnquiriesUrl"),
    URL("taxCreditsGeneralEnquiriesUrlCy", "/taxCreditsGeneralEnquiriesUrlCy"),
    URL("otherTaxesGeneralEnquiriesUrl", "/otherTaxesGeneralEnquiriesUrl"),
    URL("otherTaxesGeneralEnquiriesUrlCy", "/otherTaxesGeneralEnquiriesUrlCy"),
    URL("findRepaymentPlanUrl", "/findRepaymentPlanUrl"),
    URL("findRepaymentPlanUrlCy", "/findRepaymentPlanUrlCy"),
    URL("pensionAnnualAllowanceUrl", "/pensionAnnualAllowanceUrl"),
    URL("pensionAnnualAllowanceUrlCy", "/pensionAnnualAllowanceUrlCy"),
    URL("childBenefitDigitalAssistantUrl", "/childBenefitDigitalAssistantUrl"),
    URL("childBenefitDigitalAssistantUrlCy", "/childBenefitDigitalAssistantUrlCy"),
    URL("incomeTaxDigitalAssistantUrl", "/incomeTaxDigitalAssistantUrl"),
    URL("incomeTaxDigitalAssistantUrlCy", "/incomeTaxDigitalAssistantUrlCy"),
    URL("selfAssessmentDigitalAssistantUrl", "/selfAssessmentDigitalAssistantUrl"),
    URL("selfAssessmentDigitalAssistantUrlCy", "/selfAssessmentDigitalAssistantUrlCy"),
    URL("taxCreditsDigitalAssistantUrl", "/taxCreditsDigitalAssistantUrl"),
    URL("taxCreditsDigitalAssistantUrlCy", "/taxCreditsDigitalAssistantUrlCy"),
    URL("tcStateBenefitsUrl", "/tcStateBenefitsUrl"),
    URL("tcStateBenefitsUrlCy", "/tcStateBenefitsUrlCy"),
    URL("tcCompanyBenefitsUrl", "/tcCompanyBenefitsUrl"),
    URL("tcCompanyBenefitsUrlCy", "/tcCompanyBenefitsUrlCy"),
    URL("niAppleWalletUrl", "/niAppleWalletUrl"),
    URL("niGoogleWalletUrl", "/niGoogleWalletUrl")
  )

  val childBenefitShutteringDisabled: Shuttering = Shuttering(shuttered = false)

  def dummyConnector(
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

}
