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

package uk.gov.hmrc.mobilestartup.services
import cats.Applicative.ops.toAllApplicativeOps
import cats.implicits.catsSyntaxApplicativeId
import play.api.libs.json.Json.*
import play.api.libs.json.{JsNull, JsObject, JsString, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.TestFInstances.*
import uk.gov.hmrc.mobilestartup.model.shuttering.{Shuttering, StartupShuttering}
import uk.gov.hmrc.mobilestartup.{BaseSpec, StartupTestData}

class StartupServiceImplSpec extends BaseSpec with StartupTestData {

  "a fully successful response" should {
    "contain success entries for each service" in {

      val result: JsObject = startupService.startup("nino", journeyId, allShutteringDisabled)(HeaderCarrier()).get
      (result \ helpToSave).toOption.value                        shouldBe htsSuccessResponse
      (result \ "feature").get.as[List[FeatureFlag]]              shouldBe expectedFeatureFlags
      (result \ messages).toOption.value                          shouldBe messagesSuccessResponse
      (result \ user).toOption.value                              shouldBe userExpectedResponse
      (result \ "urls").get.as[List[URL]]                         shouldBe expectedURLs
      (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
      (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
    }
  }

  "a response" should {
    "not contain an entry for help-to-save when the hts call fails" in {
      val sut = startupService.copy(connector = dummyConnector(htsResponse = F.raiseError(new Exception("hts failed"))))
      val result: JsObject = sut.startup("nino", journeyId, allShutteringDisabled)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption                              shouldBe None
      (result \ "feature").get.as[List[FeatureFlag]]              shouldBe expectedFeatureFlags
      (result \ messages).toOption.value                          shouldBe messagesSuccessResponse
      (result \ user).toOption.value                              shouldBe userExpectedResponse
      (result \ "urls").get.as[List[URL]]                         shouldBe expectedURLs
      (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
      (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
    }

    "give successful response when there is no firstName" in {
      val sut = startupService.copy(connector = dummyConnector(citizenDetailsResponse =
        (citizenDetailsSuccessResponse.as[JsObject] +
          ("person" -> (
            (citizenDetailsSuccessResponse  \ "person").as[JsObject] +
              ("firstName" -> JsNull)
            ))).pure[TestF]
      ))
      val result: JsObject = sut.startup("nino", journeyId, allShutteringDisabled)(HeaderCarrier()).unsafeGet

      val userResponseWithNoFirstName = userExpectedResponse.as[JsObject] + ("firstName" -> JsNull) + ("name" -> JsString("John Smith"))

      (result \ helpToSave).toOption.value                        shouldBe htsSuccessResponse
      (result \ "feature").get.as[List[FeatureFlag]]              shouldBe expectedFeatureFlags
      (result \ messages).toOption.value                          shouldBe messagesSuccessResponse
      (result \ user).toOption.value                              shouldBe userResponseWithNoFirstName
      (result \ "urls").get.as[List[URL]]                         shouldBe expectedURLs
      (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
      (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
    }

    "give successful response when there is no lastName" in {
      val sut = startupService.copy(connector = dummyConnector(citizenDetailsResponse =
        (citizenDetailsSuccessResponse.as[JsObject] +
          ("person" -> (
            (citizenDetailsSuccessResponse  \ "person").as[JsObject] +
              ("lastName" -> JsNull)
            ))).pure[TestF]
      ))
      val result: JsObject = sut.startup("nino", journeyId, allShutteringDisabled)(HeaderCarrier()).unsafeGet

      val userResponseWithNoLastName = userExpectedResponse.as[JsObject] + ("lastName" -> JsNull) + ("name" -> JsString("Angus John"))

      (result \ helpToSave).toOption.value                        shouldBe htsSuccessResponse
      (result \ "feature").get.as[List[FeatureFlag]]              shouldBe expectedFeatureFlags
      (result \ messages).toOption.value                          shouldBe messagesSuccessResponse
      (result \ user).toOption.value                              shouldBe userResponseWithNoLastName
      (result \ "urls").get.as[List[URL]]                         shouldBe expectedURLs
      (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
      (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
    }


    "contain an error entry for tcr when the tcr call fails" in {
      val sut = startupService.copy(connector = dummyConnector(tcrResponse = new Exception("tcr failed").error))

      val result: JsObject = sut.startup("nino", journeyId, allShutteringDisabled)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value                        shouldBe htsSuccessResponse
      (result \ "feature").get.as[List[FeatureFlag]]              shouldBe expectedFeatureFlags
      (result \ messages).toOption.value                          shouldBe messagesSuccessResponse
      (result \ user).toOption.value                              shouldBe userExpectedResponse
      (result \ "urls").get.as[List[URL]]                         shouldBe expectedURLs
      (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
      (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
    }

    "contain an empty lists entry for messages when the messages call fails" in {
      val sut = startupService.copy(connector =
        dummyConnector(inAppMessagesResponse = new Exception("message call failed").error)
      )

      val result: JsObject = sut.startup("nino", journeyId, allShutteringDisabled)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value                        shouldBe htsSuccessResponse
      (result \ "feature").get.as[List[FeatureFlag]]              shouldBe expectedFeatureFlags
      (result \ messages).toOption.value                          shouldBe Json.parse("""{
                                                               |  "paye": [],
                                                               |  "tc": [],
                                                               |  "hts": [],
                                                               |  "tcp": []
                                                               |}
                                                               |""".stripMargin)
      (result \ user).toOption.value                              shouldBe userExpectedResponse
      (result \ "urls").get.as[List[URL]]                         shouldBe expectedURLs
      (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
      (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
    }

    "not contain an entry for user when the citizen details call fails" in {
      val sut =
        startupService.copy(connector = dummyConnector(citizenDetailsResponse = new Exception("cid failed").error))

      val result: JsObject = sut.startup("nino", journeyId, allShutteringDisabled)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value                        shouldBe htsSuccessResponse
      (result \ "feature").get.as[List[FeatureFlag]]              shouldBe expectedFeatureFlags
      (result \ messages).toOption.value                          shouldBe messagesSuccessResponse
      (result \ user).toOption                                    shouldBe None
      (result \ "urls").get.as[List[URL]]                         shouldBe expectedURLs
      (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
      (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
    }
  }

  "not contain an entry for URLs that have no value" in {
    val sut = startupService.copy(
      connector                 = dummyConnector(citizenDetailsResponse = new Exception("cid failed").error),
      cbProofOfEntitlementUrlCy = None,
      cbPaymentHistoryUrlCy     = None,
      cbChangeBankAccountUrlCy  = None,
      cbHomeUrlCy               = None,
      cbHowToClaimUrlCy         = None,
      cbFullTimeEducationUrlCy  = None,
      cbWhatChangesUrlCy        = None,
      cbTaxChargeUrlCy          = None
    )

    val result: JsObject = sut.startup("nino", journeyId, allShutteringDisabled)(HeaderCarrier()).unsafeGet

    (result \ helpToSave).toOption.value           shouldBe htsSuccessResponse
    (result \ "feature").get.as[List[FeatureFlag]] shouldBe expectedFeatureFlags
    (result \ messages).toOption.value             shouldBe messagesSuccessResponse
    (result \ user).toOption                       shouldBe None
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
      URL("otherTaxesDigitalAssistantUrlCy", "/otherTaxesDigitalAssistantUrlCy"),
      URL("payeDigitalAssistantUrl", "/payeDigitalAssistantUrl"),
      URL("payeDigitalAssistantUrlCy", "/payeDigitalAssistantUrlCy"),
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
      URL("selfAssessmentHelpAppealingPenaltiesUrl", "/selfAssessmentHelpAppealingPenaltiesUrl"),
      URL("selfAssessmentHelpAppealingPenaltiesUrlCy", "/selfAssessmentHelpAppealingPenaltiesUrlCy"),
      URL("addMissingTaxableIncomeUrl", "/addMissingTaxableIncomeUrl"),
      URL("helpToSaveGeneralEnquiriesUrl", "/helpToSaveGeneralEnquiriesUrl"),
      URL("helpToSaveGeneralEnquiriesUrlCy", "/helpToSaveGeneralEnquiriesUrlCy"),
      URL("helpToSaveDigitalAssistantUrl", "/helpToSaveDigitalAssistantUrl"),
      URL("selfAssessmentGeneralEnquiriesUrl", "/selfAssessmentGeneralEnquiriesUrl"),
      URL("selfAssessmentGeneralEnquiriesUrlCy", "/selfAssessmentGeneralEnquiriesUrlCy"),
      URL("simpleAssessmentGeneralEnquiriesUrlCy", "/simpleAssessmentGeneralEnquiriesUrlCy"),
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
    (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
    (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
  }

  "not contain an entry for user when NPS is shuttered" in {
    val result: JsObject = startupService
      .startup("nino", journeyId, StartupShuttering(Shuttering(shuttered = true), Shuttering(shuttered = false)))(
        HeaderCarrier()
      )
      .unsafeGet

    (result \ helpToSave).toOption.value                        shouldBe htsSuccessResponse
    (result \ "feature").get.as[List[FeatureFlag]]              shouldBe expectedFeatureFlags
    (result \ messages).toOption.value                          shouldBe messagesSuccessResponse
    (result \ user).toOption                                    shouldBe None
    (result \ "urls").get.as[List[URL]]                         shouldBe expectedURLs
    (result \ "childBenefit" \ "shuttering").get.as[Shuttering] shouldBe childBenefitShutteringDisabled
    (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
  }

  "Return shuttered details when Child Benefit is shuttered" in {
    val result: JsObject = startupService
      .startup(
        "nino",
        journeyId,
        StartupShuttering(Shuttering(shuttered = false),
                          Shuttering(shuttered = true,
                                     title     = Some("Shuttered"),
                                     message   = Some("Sorry, this is not available")))
      )(
        HeaderCarrier()
      )
      .unsafeGet

    (result \ helpToSave).toOption.value                                   shouldBe htsSuccessResponse
    (result \ "feature").get.as[List[FeatureFlag]]                         shouldBe expectedFeatureFlags
    (result \ messages).toOption.value                                     shouldBe messagesSuccessResponse
    (result \ user).toOption.value                                         shouldBe userExpectedResponse
    (result \ "urls").get.as[List[URL]]                                    shouldBe expectedURLs
    (result \ "childBenefit" \ "shuttering" \ "shuttered").get.as[Boolean] shouldBe true
    (result \ "childBenefit" \ "shuttering" \ "title").get.as[String]      shouldBe "Shuttered"
    (result \ "childBenefit" \ "shuttering" \ "message").get.as[String]    shouldBe "Sorry, this is not available"
    (result \ "throttleValue").get.as[List[ThrottleValue]]      shouldBe expectedThrottleValue
  }
}
