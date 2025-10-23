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

package uk.gov.hmrc.mobilestartup.config

import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, TypeLiteral}
import javax.inject.Inject
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.mobilestartup.connectors.{GenericConnector, GenericConnectorImpl}
import uk.gov.hmrc.mobilestartup.controllers.api.ApiAccess
import uk.gov.hmrc.mobilestartup.services.{LivePreFlightService, LiveStartupService, PreFlightService, StartupService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import scala.concurrent.Future

class GuiceModule @Inject() (
  environment:   Environment,
  configuration: Configuration)
    extends AbstractModule {

  val servicesConfig: ServicesConfig = new ServicesConfig(configuration)

  override def configure(): Unit = {

    bindConfigInt("controllers.confidenceLevel")
    bindConfigString("appUrl", "appUrl")
    bindConfigBoolean("feature.userPanelSignUp")
    bindConfigBoolean("feature.enablePushNotificationTokenRegistration")
    bindConfigBoolean("feature.paperlessAlertDialogs")
    bindConfigBoolean("feature.paperlessAdverts")
    bindConfigBoolean("feature.htsAdverts")
    bindConfigBoolean("feature.customerSatisfactionSurveys")
    bindConfigBoolean("feature.findMyNinoAddToWallet")
    bindConfigBoolean("feature.findMyNinoAddToGoogleWallet")
    bindConfigBoolean("feature.useNudgeComm")
    bindConfigBoolean("feature.enableChangeOfBankPegaURL")
    bindConfigBoolean("feature.enableProofOfEntitlementPegaURL")
    bindConfigBoolean("feature.enableTaxCreditEndBanner")
    bindConfigBoolean("feature.enableBPPCardViews")
    bindConfigBoolean("feature.disableYourEmploymentIncomeChart")
    bindConfigBoolean("feature.disableYourEmploymentIncomeChartAndroid")
    bindConfigBoolean("feature.disableYourEmploymentIncomeChartIos")
    bindConfigBoolean("feature.annualTaxSummaryLink")
    bindConfigBoolean("enableMultipleGGIDCheck.ios")
    bindConfigBoolean("enableMultipleGGIDCheck.android")
    bindConfigOptionalString("cbProofOfEntitlementUrl", "url.cbProofOfEntitlementUrl")
    bindConfigOptionalString("cbProofOfEntitlementUrlCy", "url.cbProofOfEntitlementUrlCy")
    bindConfigOptionalString("cbPaymentHistoryUrl", "url.cbPaymentHistoryUrl")
    bindConfigOptionalString("cbPaymentHistoryUrlCy", "url.cbPaymentHistoryUrlCy")
    bindConfigOptionalString("cbChangeBankAccountUrl", "url.cbChangeBankAccountUrl")
    bindConfigOptionalString("cbChangeBankAccountUrlCy", "url.cbChangeBankAccountUrlCy")
    bindConfigOptionalString("cbHomeUrl", "url.cbHomeUrl")
    bindConfigOptionalString("cbHomeUrlCy", "url.cbHomeUrlCy")
    bindConfigOptionalString("cbHowToClaimUrl", "url.cbHowToClaimUrl")
    bindConfigOptionalString("cbHowToClaimUrlCy", "url.cbHowToClaimUrlCy")
    bindConfigOptionalString("cbFullTimeEducationUrl", "url.cbFullTimeEducationUrl")
    bindConfigOptionalString("cbFullTimeEducationUrlCy", "url.cbFullTimeEducationUrlCy")
    bindConfigOptionalString("cbWhatChangesUrl", "url.cbWhatChangesUrl")
    bindConfigOptionalString("cbWhatChangesUrlCy", "url.cbWhatChangesUrlCy")
    bindConfigOptionalString("statePensionUrl", "url.statePensionUrl")
    bindConfigOptionalString("niSummaryUrl", "url.niSummaryUrl")
    bindConfigOptionalString("niContributionsUrl", "url.niContributionsUrl")
    bindConfigOptionalString("otherTaxesDigitalAssistantUrl", "url.otherTaxesDigitalAssistantUrl")
    bindConfigOptionalString("otherTaxesDigitalAssistantUrlCy", "url.otherTaxesDigitalAssistantUrlCy")
    bindConfigOptionalString("payeDigitalAssistantUrl", "url.payeDigitalAssistantUrl")
    bindConfigOptionalString("payeDigitalAssistantUrlCy", "url.payeDigitalAssistantUrlCy")
    bindConfigOptionalString("learnAboutCallChargesUrl", "url.learnAboutCallChargesUrl")
    bindConfigOptionalString("learnAboutCallChargesUrlCy", "url.learnAboutCallChargesUrlCy")
    bindConfigOptionalString("statePensionAgeUrl", "url.statePensionAgeUrl")
    bindConfigOptionalString("tcNationalInsuranceRatesLettersUrl", "url.tcNationalInsuranceRatesLettersUrl")
    bindConfigOptionalString("tcNationalInsuranceRatesLettersUrlCy", "url.tcNationalInsuranceRatesLettersUrlCy")
    bindConfigOptionalString("tcPersonalAllowanceUrl", "url.tcPersonalAllowanceUrl")
    bindConfigOptionalString("tcPersonalAllowanceUrlCy", "url.tcPersonalAllowanceUrlCy")
    bindConfigOptionalString("scottishIncomeTaxUrl", "url.scottishIncomeTaxUrl")
    bindConfigOptionalString("scottishIncomeTaxUrlCy", "url.scottishIncomeTaxUrlCy")
    bindConfigOptionalString("cbTaxChargeUrl", "url.cbTaxChargeUrl")
    bindConfigOptionalString("cbTaxChargeUrlCy", "url.cbTaxChargeUrlCy")
    bindConfigOptionalString("selfAssessmentHelpAppealingPenaltiesUrl", "url.selfAssessmentHelpAppealingPenaltiesUrl")
    bindConfigOptionalString("selfAssessmentHelpAppealingPenaltiesUrlCy",
                             "url.selfAssessmentHelpAppealingPenaltiesUrlCy")
    bindConfigOptionalString("addMissingTaxableIncomeUrl", "url.addMissingTaxableIncomeUrl")
    bindConfigOptionalString("helpToSaveGeneralEnquiriesUrl", "url.helpToSaveGeneralEnquiriesUrl")
    bindConfigOptionalString("helpToSaveGeneralEnquiriesUrlCy", "url.helpToSaveGeneralEnquiriesUrlCy")
    bindConfigOptionalString("helpToSaveDigitalAssistantUrl", "url.helpToSaveDigitalAssistantUrl")
    bindConfigOptionalString("selfAssessmentGeneralEnquiriesUrl", "url.selfAssessmentGeneralEnquiriesUrl")
    bindConfigOptionalString("selfAssessmentGeneralEnquiriesUrlCy", "url.selfAssessmentGeneralEnquiriesUrlCy")
    bindConfigOptionalString("simpleAssessmentGeneralEnquiriesUrlCy", "url.simpleAssessmentGeneralEnquiriesUrlCy")
    bindConfigOptionalString("findRepaymentPlanUrl", "url.findRepaymentPlanUrl")
    bindConfigOptionalString("findRepaymentPlanUrlCy", "url.findRepaymentPlanUrlCy")
    bindConfigOptionalString("pensionAnnualAllowanceUrl", "url.pensionAnnualAllowanceUrl")
    bindConfigOptionalString("pensionAnnualAllowanceUrlCy", "url.pensionAnnualAllowanceUrlCy")
    bindConfigOptionalString("childBenefitDigitalAssistantUrl", "url.childBenefitDigitalAssistantUrl")
    bindConfigOptionalString("childBenefitDigitalAssistantUrlCy", "url.childBenefitDigitalAssistantUrlCy")
    bindConfigOptionalString("incomeTaxDigitalAssistantUrl", "url.incomeTaxDigitalAssistantUrl")
    bindConfigOptionalString("incomeTaxDigitalAssistantUrlCy", "url.incomeTaxDigitalAssistantUrlCy")
    bindConfigOptionalString("selfAssessmentDigitalAssistantUrl", "url.selfAssessmentDigitalAssistantUrl")
    bindConfigOptionalString("selfAssessmentDigitalAssistantUrlCy", "url.selfAssessmentDigitalAssistantUrlCy")
    bindConfigOptionalString("taxCreditsDigitalAssistantUrl", "url.taxCreditsDigitalAssistantUrl")
    bindConfigOptionalString("taxCreditsDigitalAssistantUrlCy", "url.taxCreditsDigitalAssistantUrlCy")
    bindConfigOptionalString("tcStateBenefitsUrl", "url.tcStateBenefitsUrl")
    bindConfigOptionalString("tcStateBenefitsUrlCy", "url.tcStateBenefitsUrlCy")
    bindConfigOptionalString("tcCompanyBenefitsUrl", "url.tcCompanyBenefitsUrl")
    bindConfigOptionalString("tcCompanyBenefitsUrlCy", "url.tcCompanyBenefitsUrlCy")
    bindConfigOptionalString("otherTaxesGeneralEnquiriesUrl", "url.otherTaxesGeneralEnquiriesUrl")
    bindConfigOptionalString("otherTaxesGeneralEnquiriesUrlCy", "url.otherTaxesGeneralEnquiriesUrlCy")
    bindConfigOptionalString("niAppleWalletUrl", "url.niAppleWalletUrl")
    bindConfigOptionalString("niGoogleWalletUrl", "url.niGoogleWalletUrl")
    bindConfigBoolean("feature.enableTaxCreditShuttering")
    bindConfigBoolean("feature.enableUniversalPensionTaxCreditBanner")
    bindConfigBoolean("feature.enableHtsBanner")
    bindConfigString("bannerStartTime", "htsBannerDisplayTimings.startTime")
    bindConfigString("bannerEndTime", "htsBannerDisplayTimings.endTime")
    bindConfigBoolean("feature.enableChildBenefitMVP")
    bindConfigBoolean("feature.enableStudentLoanPlanTypeFive")
    bindConfigBoolean("feature.enableNewCreatePINScreenFlow")
    bindConfigBoolean("feature.enablePinSecurity")
    bind(classOf[String])
      .annotatedWith(named("mobile-shuttering"))
      .toInstance(servicesConfig.baseUrl("mobile-shuttering"))
    bind(classOf[Logger]).toInstance(Logger(this.getClass))
    bindConfigString("storeReviewAccountInternalId", "demoAccounts.storeReviewId")
    bindConfigString("appTeamAccountInternalId", "demoAccounts.appTeamId")

    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])

    bind(new TypeLiteral[GenericConnector[Future]] {}).to(classOf[GenericConnectorImpl])
    bind(new TypeLiteral[StartupService[Future]] {}).to(classOf[LiveStartupService])
    bind(new TypeLiteral[PreFlightService[Future]] {}).to(classOf[LivePreFlightService])

    bind(classOf[ApiAccess]).toInstance(ApiAccess("PRIVATE"))
  }

  /**
    * Binds a configuration value using the `path` as the name for the binding.
    * Throws an exception if the configuration value does not exist or cannot be read as an Int.
    */
  private def bindConfigInt(path: String): Unit =
    bindConstant().annotatedWith(named(path)).to(configuration.underlying.getInt(path))

  private def bindConfigString(
    name: String,
    path: String
  ): Unit =
    bindConstant().annotatedWith(named(name)).to(configuration.underlying.getString(path))

  private def bindConfigOptionalString(
    name: String,
    path: String
  ): Unit = {
    val configValue: Option[String] = configuration
      .getOptional[String](path)
    bind(new TypeLiteral[Option[String]] {})
      .annotatedWith(named(name))
      .toInstance(configValue)
  }

  private def bindConfigBoolean(path: String): Unit =
    bindConstant().annotatedWith(named(path)).to(configuration.underlying.getBoolean(path))
}
