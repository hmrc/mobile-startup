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

package uk.gov.hmrc.mobilestartup.support

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.OptionValues
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import eu.timepit.refined.auto._

import scala.concurrent.Future

class BaseISpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout {

  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader:        (String, String) = "Accept"        -> "application/vnd.hmrc.1.0+json"
  protected val authorizationJsonHeader: (String, String) = "AUTHORIZATION" -> "Bearer test"

  protected val userAgentJsonHeaderIos
    : (String, String) = "user-agent" -> "HMRCNextGenConsumer/uk.gov.hmrc.TaxCalc 14.12.0 (iOS 16.1.1)"

  protected val userAgentJsonHeaderAndroid
    : (String, String) = "user-agent" -> "HMRCNextGenConsumer/uk.gov.hmrc.TaxCalc 15.3.0 (Android 10; SM-G960F Build/QP1A.190711.020)"

  val nino:      Nino      = Nino("AA000006C")
  val saUtr:     SaUtr     = SaUtr("123456789")
  val journeyId: JourneyId = "27085215-69a4-4027-8f72-b04b10ec16b0"
  val url:       String    = s"/preflight-check?journeyId=$journeyId"

  def getRequestWithAcceptHeader(url: String): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader, authorizationJsonHeader, userAgentJsonHeaderIos).get()

  def config: Map[String, Any] =
    Map(
      "auditing.enabled"                                      -> false,
      "microservice.services.auth.port"                       -> wireMockPort,
      "microservice.services.datastream.port"                 -> wireMockPort,
      "microservice.services.mobile-tax-credits-renewal.port" -> wireMockPort,
      "microservice.services.citizen-details.port"            -> wireMockPort,
      "microservice.services.enrolment-store-proxy.port"      -> wireMockPort,
      "microservice.services.mobile-shuttering.port"          -> wireMockPort,
      "auditing.consumer.baseUri.port"                        -> wireMockPort,
      "feature.userPanelSignUp"                               -> true,
      "feature.enablePushNotificationTokenRegistration"       -> true,
      "feature.helpToSave.enableBadge"                        -> true,
      "feature.paperlessAlertDialogs"                         -> true,
      "feature.paperlessAdverts"                              -> true,
      "feature.htsAdverts"                                    -> true,
      "feature.saTile"                                        -> true,
      "feature.annualTaxSummaryLink"                          -> true,
      "enableMultipleGGIDCheck.ios"                           -> true,
      "enableMultipleGGIDCheck.android"                       -> true,
      "feature.customerSatisfactionSurveys"                   -> true,
      "feature.findMyNinoAddToWallet"                         -> true,
      "feature.disableYourEmploymentIncomeChart"              -> false,
      "feature.disableYourEmploymentIncomeChartAndroid"       -> false,
      "feature.disableYourEmploymentIncomeChartIos"           -> false,
      "feature.findMyNinoAddToGoogleWallet"                   -> true,
      "url.cbProofOfEntitlementUrl"                           -> "/child-benefit/view-proof-entitlement",
      "url.cbProofOfEntitlementUrlCy"                         -> "/child-benefit/view-proof-entitlementCy",
      "url.cbPaymentHistoryUrl"                               -> "/child-benefit/view-payment-history",
      "url.cbPaymentHistoryUrlCy"                             -> "/child-benefit/view-payment-historyCy",
      "url.cbHomeUrl"                                         -> "/child-benefit/home",
      "url.cbHomeUrlCy"                                       -> "/child-benefit/homeCy",
      "url.cbHowToClaimUrl"                                   -> "/child-benefit/how-to-claim",
      "url.cbHowToClaimUrlCy"                                 -> "/child-benefit/how-to-claimCy",
      "url.cbFullTimeEducationUrl"                            -> "/gov-uk/child-benefit-16-19",
      "url.cbFullTimeEducationUrlCy"                          -> "/gov-uk/child-benefit-16-19Cy",
      "url.cbWhatChangesUrl"                                  -> "/personal-account/child-benefit-forms",
      "url.cbWhatChangesUrlCy"                                -> "/personal-account/child-benefit-formsCy",
      "url.statePensionUrl"                                   -> "/statePensionUrl",
      "url.niSummaryUrl"                                      -> "/niSummaryUrl",
      "url.niContributionsUrl"                                -> "/niContributionsUrl",
      "url.otherTaxesDigitalAssistantUrl"                     -> "/otherTaxesDigitalAssistantUrl",
      "url.otherTaxesDigitalAssistantUrlCy"                   -> "/otherTaxesDigitalAssistantUrlCy",
      "url.payeDigitalAssistantUrl"                           -> "/payeDigitalAssistantUrl",
      "url.payeDigitalAssistantUrlCy"                         -> "/payeDigitalAssistantUrlCy",
      "url.incomeTaxGeneralEnquiriesUrl"                      -> "/incomeTaxGeneralEnquiriesUrl",
      "url.learnAboutCallChargesUrl"                          -> "/learnAboutCallChargesUrl",
      "url.learnAboutCallChargesUrlCy"                        -> "/learnAboutCallChargesUrlCy",
      "url.statePensionAgeUrl"                                -> "/statePensionAgeUrl",
      "url.tcNationalInsuranceRatesLettersUrl"                -> "/tcNationalInsuranceRatesLettersUrl",
      "url.tcNationalInsuranceRatesLettersUrlCy"              -> "/tcNationalInsuranceRatesLettersUrlCy",
      "url.tcPersonalAllowanceUrl"                            -> "/tcPersonalAllowanceUrl",
      "url.tcPersonalAllowanceUrlCy"                          -> "/tcPersonalAllowanceUrlCy",
      "url.scottishIncomeTaxUrl"                              -> "/scottishIncomeTaxUrl",
      "url.scottishIncomeTaxUrlCy"                            -> "/scottishIncomeTaxUrlCy",
      "url.cbTaxChargeUrl"                                    -> "/cbTaxChargeUrl",
      "url.cbTaxChargeUrlCy"                                  -> "/cbTaxChargeUrlCy",
      "url.selfAssessmentHelpAppealingPenaltiesUrl"           -> "/selfAssessmentHelpAppealingPenaltiesUrl",
      "url.selfAssessmentHelpAppealingPenaltiesUrlCy"         -> "/selfAssessmentHelpAppealingPenaltiesUrlCy",
      "url.addMissingTaxableIncomeUrl"                        -> "/addMissingTaxableIncomeUrl",
      "url.helpToSaveGeneralEnquiriesUrl"                     -> "/helpToSaveGeneralEnquiriesUrl",
      "url.helpToSaveGeneralEnquiriesUrlCy"                   -> "/helpToSaveGeneralEnquiriesUrlCy",
      "url.helpToSaveDigitalAssistantUrl"                     -> "/helpToSaveDigitalAssistantUrl",
      "url.selfAssessmentGeneralEnquiriesUrl"                 -> "/selfAssessmentGeneralEnquiriesUrl",
      "url.selfAssessmentGeneralEnquiriesUrlCy"               -> "/selfAssessmentGeneralEnquiriesUrlCy",
      "url.simpleAssessmentGeneralEnquiriesUrl"               -> "/simpleAssessmentGeneralEnquiriesUrl",
      "url.simpleAssessmentGeneralEnquiriesUrlCy"             -> "/simpleAssessmentGeneralEnquiriesUrlCy",
      "url.findRepaymentPlanUrl"                              -> "/findRepaymentPlanUrl",
      "url.findRepaymentPlanUrlCy"                            -> "/findRepaymentPlanUrlCy",
      "url.pensionAnnualAllowanceUrl"                         -> "/pensionAnnualAllowanceUrl",
      "url.pensionAnnualAllowanceUrlCy"                       -> "/pensionAnnualAllowanceUrlCy",
      "url.childBenefitDigitalAssistantUrl"                   -> "/childBenefitDigitalAssistantUrl",
      "url.childBenefitDigitalAssistantUrlCy"                 -> "/childBenefitDigitalAssistantUrlCy",
      "url.incomeTaxDigitalAssistantUrl"                      -> "/incomeTaxDigitalAssistantUrl",
      "url.incomeTaxDigitalAssistantUrlCy"                    -> "/incomeTaxDigitalAssistantUrlCy",
      "url.selfAssessmentDigitalAssistantUrl"                 -> "/selfAssessmentDigitalAssistantUrl",
      "url.selfAssessmentDigitalAssistantUrlCy"               -> "/selfAssessmentDigitalAssistantUrlCy",
      "url.taxCreditsDigitalAssistantUrl"                     -> "/taxCreditsDigitalAssistantUrl",
      "url.taxCreditsDigitalAssistantUrlCy"                   -> "/taxCreditsDigitalAssistantUrlCy",
      "url.tcStateBenefitsUrl"                                -> "/tcStateBenefitsUrl",
      "url.tcStateBenefitsUrlCy"                              -> "/tcStateBenefitsUrlCy",
      "url.tcCompanyBenefitsUrl"                              -> "/tcCompanyBenefitsUrl",
      "url.tcCompanyBenefitsUrlCy"                            -> "/tcCompanyBenefitsUrlCy",
      "demoAccounts.storeReviewId"                            -> "storeReviewAccountId",
      "demoAccounts.appTeamId"                                -> "appTeamAccountId"
    )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
