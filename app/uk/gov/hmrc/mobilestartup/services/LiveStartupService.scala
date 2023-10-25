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
import com.google.inject.name.Named
import javax.inject.Inject
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector

import scala.concurrent.{ExecutionContext, Future}

class LiveStartupService @Inject() (
  connector:                                                                                                                        GenericConnector[Future],
  @Named("feature.userPanelSignUp") userPanelSignUp:                                                                                Boolean,
  @Named("feature.enablePushNotificationTokenRegistration") enablePushNotificationTokenRegistration:                                Boolean,
  @Named("feature.paperlessAlertDialogs") enablePaperlessAlertDialogs:                                                              Boolean,
  @Named("feature.paperlessAdverts") enablePaperlessAdverts:                                                                        Boolean,
  @Named("feature.htsAdverts") enableHtsAdverts:                                                                                    Boolean,
  @Named("feature.annualTaxSummaryLink") enableAnnualTaxSummaryLink:                                                                Boolean,
  @Named("cbProofOfEntitlementUrl") cbProofOfEntitlementUrl:                                                                        Option[String],
  @Named("cbProofOfEntitlementUrlCy") cbProofOfEntitlementUrlCy:                                                                    Option[String],
  @Named("cbPaymentHistoryUrl") cbPaymentHistoryUrl:                                                                                Option[String],
  @Named("cbPaymentHistoryUrlCy") cbPaymentHistoryUrlCy:                                                                            Option[String],
  @Named("cbChangeBankAccountUrl") cbChangeBankAccountUrl:                                                                          Option[String],
  @Named("cbChangeBankAccountUrlCy") cbChangeBankAccountUrlCy:                                                                      Option[String],
  @Named("cbHomeUrl") cbHomeUrl:                                                                                                    Option[String],
  @Named("cbHomeUrlCy") cbHomeUrlCy:                                                                                                Option[String],
  @Named("cbHowToClaimUrl") cbHowToClaimUrl:                                                                                        Option[String],
  @Named("cbHowToClaimUrlCy") cbHowToClaimUrlCy:                                                                                    Option[String],
  @Named("cbFullTimeEducationUrl") cbFullTimeEducationUrl:                                                                          Option[String],
  @Named("cbFullTimeEducationUrlCy") cbFullTimeEducationUrlCy:                                                                      Option[String],
  @Named("cbWhatChangesUrl") cbWhatChangesUrl:                                                                                      Option[String],
  @Named("cbWhatChangesUrlCy") cbWhatChangesUrlCy:                                                                                  Option[String],
  @Named("statePensionUrl") statePensionUrl:                                                                                        Option[String],
  @Named("niSummaryUrl") niSummaryUrl:                                                                                              Option[String],
  @Named("niContributionsUrl") niContributionsUrl:                                                                                  Option[String],
  @Named("otherTaxesDigitalAssistantUrl") otherTaxesDigitalAssistantUrl:                                                            Option[String],
  @Named("otherTaxesDigitalAssistantUrlCy") otherTaxesDigitalAssistantUrlCy:                                                        Option[String],
  @Named("feature.customerSatisfactionSurveys") enableCustomerSatisfactionSurveys:                                                  Boolean,
  @Named("feature.findMyNinoAddToWallet") findMyNinoAddToWallet:                                                                      Boolean,
  @Named("feature.disableYourEmploymentIncomeChart") disableYourEmploymentIncomeChart:                                              Boolean,
  @Named("feature.disableYourEmploymentIncomeChartAndroid") disableYourEmploymentIncomeChartAndroid:                                Boolean,
  @Named("feature.disableYourEmploymentIncomeChartIos") disableYourEmploymentIncomeChartIos:                                        Boolean,
  @Named("feature.findMyNinoAddToGoogleWallet") findMyNinoAddToGoogleWallet:                                                          Boolean
)(implicit ec:                                                                                                                      ExecutionContext)
    extends StartupServiceImpl[Future](connector,
                                       userPanelSignUp,
                                       enablePushNotificationTokenRegistration,
                                       enablePaperlessAlertDialogs,
                                       enablePaperlessAdverts,
                                       enableHtsAdverts,
                                       enableAnnualTaxSummaryLink,
                                       cbProofOfEntitlementUrl,
                                       cbProofOfEntitlementUrlCy,
                                       cbPaymentHistoryUrl,
                                       cbPaymentHistoryUrlCy,
                                       cbChangeBankAccountUrl,
                                       cbChangeBankAccountUrlCy,
                                       cbHomeUrl,
                                       cbHomeUrlCy,
                                       cbHowToClaimUrl,
                                       cbHowToClaimUrlCy,
                                       cbFullTimeEducationUrl,
                                       cbFullTimeEducationUrlCy,
                                       cbWhatChangesUrl,
                                       cbWhatChangesUrlCy,
                                       statePensionUrl,
                                       niSummaryUrl,
                                       niContributionsUrl,
                                       otherTaxesDigitalAssistantUrl,
                                       otherTaxesDigitalAssistantUrlCy,
                                       enableCustomerSatisfactionSurveys,
                                       findMyNinoAddToWallet,
                                       disableYourEmploymentIncomeChart,
                                       disableYourEmploymentIncomeChartAndroid,
                                       disableYourEmploymentIncomeChartIos,
                                       findMyNinoAddToGoogleWallet)
