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

import cats.MonadError
import cats.implicits._

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.PersonDetails
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import play.api.http.Status.LOCKED
import uk.gov.hmrc.mobilestartup.model.shuttering.{Shuttering, StartupShuttering}

import scala.util.control.NonFatal

case class FeatureFlag(
  name:    String,
  enabled: Boolean)

object FeatureFlag {
  implicit val formats: Format[FeatureFlag] = Json.format[FeatureFlag]
}

case class URL(
  name: String,
  url:  String)

object URL {
  implicit val formats: Format[URL] = Json.format[URL]
}

/**
  * Decided to implement this generically using Tagless as an example of how it can be introduced
  * into a codebase without necessarily converting everything. It did require introducing a type parameter
  * onto the `GenericConnector` trait but that had very little impact beyond the change to the guice wiring.
  */
case class StartupServiceImpl[F[_]] @Inject() (
  connector:                                 GenericConnector[F],
  userPanelSignUp:                           Boolean,
  enablePushNotificationTokenRegistration:   Boolean,
  enablePaperlessAlertDialogs:               Boolean,
  enablePaperlessAdverts:                    Boolean,
  enableHtsAdverts:                          Boolean,
  enableAnnualTaxSummaryLink:                Boolean,
  cbProofOfEntitlementUrl:                   Option[String],
  cbProofOfEntitlementUrlCy:                 Option[String],
  cbPaymentHistoryUrl:                       Option[String],
  cbPaymentHistoryUrlCy:                     Option[String],
  cbChangeBankAccountUrl:                    Option[String],
  cbChangeBankAccountUrlCy:                  Option[String],
  cbHomeUrl:                                 Option[String],
  cbHomeUrlCy:                               Option[String],
  cbHowToClaimUrl:                           Option[String],
  cbHowToClaimUrlCy:                         Option[String],
  cbFullTimeEducationUrl:                    Option[String],
  cbFullTimeEducationUrlCy:                  Option[String],
  cbWhatChangesUrl:                          Option[String],
  cbWhatChangesUrlCy:                        Option[String],
  statePensionUrl:                           Option[String],
  niSummaryUrl:                              Option[String],
  niContributionsUrl:                        Option[String],
  otherTaxesDigitalAssistantUrl:             Option[String],
  otherTaxesDigitalAssistantUrlCy:           Option[String],
  payeDigitalAssistantUrl:                   Option[String],
  payeDigitalAssistantUrlCy:                 Option[String],
  incomeTaxGeneralEnquiriesUrl:              Option[String],
  incomeTaxGeneralEnquiriesUrlCy:            Option[String],
  learnAboutCallChargesUrl:                  Option[String],
  learnAboutCallChargesUrlCy:                Option[String],
  statePensionAgeUrl:                        Option[String],
  tcNationalInsuranceRatesLettersUrl:        Option[String],
  tcNationalInsuranceRatesLettersUrlCy:      Option[String],
  tcPersonalAllowanceUrl:                    Option[String],
  tcPersonalAllowanceUrlCy:                  Option[String],
  scottishIncomeTaxUrl:                      Option[String],
  scottishIncomeTaxUrlCy:                    Option[String],
  cbTaxChargeUrl:                            Option[String],
  cbTaxChargeUrlCy:                          Option[String],
  selfAssessmentHelpAppealingPenaltiesUrl:   Option[String],
  selfAssessmentHelpAppealingPenaltiesUrlCy: Option[String],
  addMissingTaxableIncomeUrl:                Option[String],
  helpToSaveGeneralEnquiriesUrl:             Option[String],
  helpToSaveGeneralEnquiriesUrlCy:           Option[String],
  helpToSaveDigitalAssistantUrl:             Option[String],
  selfAssessmentGeneralEnquiriesUrl:         Option[String],
  selfAssessmentGeneralEnquiriesUrlCy:       Option[String],
  simpleAssessmentGeneralEnquiriesUrl:       Option[String],
  simpleAssessmentGeneralEnquiriesUrlCy:     Option[String],
  cbGeneralEnquiriesUrl:                     Option[String],
  cbGeneralEnquiriesUrlCy:                   Option[String],
  taxCreditsGeneralEnquiriesUrl:             Option[String],
  taxCreditsGeneralEnquiriesUrlCy:           Option[String],
  otherTaxesGeneralEnquiriesUrl:             Option[String],
  otherTaxesGeneralEnquiriesUrlCy:           Option[String],
  findRepaymentPlanUrl:                      Option[String],
  findRepaymentPlanUrlCy:                    Option[String],
  pensionAnnualAllowanceUrl:                 Option[String],
  pensionAnnualAllowanceUrlCy:               Option[String],
  childBenefitDigitalAssistantUrl:           Option[String],
  childBenefitDigitalAssistantUrlCy:         Option[String],
  incomeTaxDigitalAssistantUrl:              Option[String],
  incomeTaxDigitalAssistantUrlCy:            Option[String],
  selfAssessmentDigitalAssistantUrl:         Option[String],
  selfAssessmentDigitalAssistantUrlCy:       Option[String],
  taxCreditsDigitalAssistantUrl:             Option[String],
  taxCreditsDigitalAssistantUrlCy:           Option[String],
  tcStateBenefitsUrl:                        Option[String],
  tcStateBenefitsUrlCy:                      Option[String],
  tcCompanyBenefitsUrl:                      Option[String],
  tcCompanyBenefitsUrlCy:                    Option[String],
  niAppleWalletUrl:                          Option[String],
  niGoogleWalletUrl:                         Option[String],
  enableCustomerSatisfactionSurveys:         Boolean,
  findMyNinoAddToWallet:                     Boolean,
  disableYourEmploymentIncomeChart:          Boolean,
  disableYourEmploymentIncomeChartAndroid:   Boolean,
  disableYourEmploymentIncomeChartIos:       Boolean,
  findMyNinoAddToGoogleWallet:               Boolean,
  disableOldTaxCalculator:                   Boolean,
  useNudgeComm:                              Boolean,
  enableChangeOfBankPegaURL:                 Boolean,
  enableTaxCreditEndBanner:                  Boolean
)(implicit F:                                MonadError[F, Throwable])
    extends StartupService[F] {

  val logger: Logger = Logger(this.getClass)

  override def startup(
    nino:               String,
    journeyId:          JourneyId,
    shutteringStatuses: StartupShuttering
  )(implicit hc:        HeaderCarrier
  ): F[JsObject] =
    (callService("helpToSave")(mhtsStartup),
     callService("taxCreditRenewals")(tcrStartup(journeyId)),
     callService("messages")(inAppMsgsStartup),
     callService("user")(citizenDetailsStartup(nino, shutteringStatuses.npsShuttering.shuttered)),
     childBenefitStartup(shutteringStatuses.childBenefitShuttering).pure[F],
     featureFlags.pure[F],
     urls.pure[F]).mapN((a, b, c, d, e, f, g) => a ++ b ++ c ++ d ++ e ++ f ++ g)

  private val featureFlags: JsObject =
    obj(
      "feature" -> List(
        FeatureFlag("userPanelSignUp", userPanelSignUp),
        FeatureFlag("enablePushNotificationTokenRegistration", enablePushNotificationTokenRegistration),
        FeatureFlag("paperlessAlertDialogs", enablePaperlessAlertDialogs),
        FeatureFlag("paperlessAdverts", enablePaperlessAdverts),
        FeatureFlag("htsAdverts", enableHtsAdverts),
        FeatureFlag("annualTaxSummaryLink", enableAnnualTaxSummaryLink),
        FeatureFlag("customerSatisfactionSurveys", enableCustomerSatisfactionSurveys),
        FeatureFlag("findMyNinoAddToWallet", findMyNinoAddToWallet),
        FeatureFlag("disableYourEmploymentIncomeChart", disableYourEmploymentIncomeChart),
        FeatureFlag("disableYourEmploymentIncomeChartAndroid", disableYourEmploymentIncomeChartAndroid),
        FeatureFlag("disableYourEmploymentIncomeChartIos", disableYourEmploymentIncomeChartIos),
        FeatureFlag("findMyNinoAddToGoogleWallet", findMyNinoAddToGoogleWallet),
        FeatureFlag("disableOldTaxCalculator", disableOldTaxCalculator),
        FeatureFlag("useNudgeComm", useNudgeComm),
        FeatureFlag("enableChangeOfBankPegaURL", enableChangeOfBankPegaURL),
        FeatureFlag("enableTaxCreditEndBanner", enableTaxCreditEndBanner),
        FeatureFlag("annualTaxSummaryLink", enableAnnualTaxSummaryLink)
      )
    )

  private val urls: JsObject =
    obj(
      "urls" -> List(
        cbProofOfEntitlementUrl.map(URL("cbProofOfEntitlementUrl", _)),
        cbProofOfEntitlementUrlCy.map(URL("cbProofOfEntitlementUrlCy", _)),
        cbPaymentHistoryUrl.map(URL("cbPaymentHistoryUrl", _)),
        cbPaymentHistoryUrlCy.map(URL("cbPaymentHistoryUrlCy", _)),
        cbChangeBankAccountUrl.map(URL("cbChangeBankAccountUrl", _)),
        cbChangeBankAccountUrlCy.map(URL("cbChangeBankAccountUrlCy", _)),
        cbHomeUrl.map(URL("cbHomeUrl", _)),
        cbHomeUrlCy.map(URL("cbHomeUrlCy", _)),
        cbHowToClaimUrl.map(URL("cbHowToClaimUrl", _)),
        cbHowToClaimUrlCy.map(URL("cbHowToClaimUrlCy", _)),
        cbFullTimeEducationUrl.map(URL("cbFullTimeEducationUrl", _)),
        cbFullTimeEducationUrlCy.map(URL("cbFullTimeEducationUrlCy", _)),
        cbWhatChangesUrl.map(URL("cbWhatChangesUrl", _)),
        cbWhatChangesUrlCy.map(URL("cbWhatChangesUrlCy", _)),
        statePensionUrl.map(URL("statePensionUrl", _)),
        niSummaryUrl.map(URL("niSummaryUrl", _)),
        niContributionsUrl.map(URL("niContributionsUrl", _)),
        otherTaxesDigitalAssistantUrl.map(URL("otherTaxesDigitalAssistantUrl", _)),
        otherTaxesDigitalAssistantUrlCy.map(URL("otherTaxesDigitalAssistantUrlCy", _)),
        payeDigitalAssistantUrl.map(URL("payeDigitalAssistantUrl", _)),
        payeDigitalAssistantUrlCy.map(URL("payeDigitalAssistantUrlCy", _)),
        incomeTaxGeneralEnquiriesUrl.map(URL("incomeTaxGeneralEnquiriesUrl", _)),
        incomeTaxGeneralEnquiriesUrlCy.map(URL("incomeTaxGeneralEnquiriesUrlCy", _)),
        learnAboutCallChargesUrl.map(URL("learnAboutCallChargesUrl", _)),
        learnAboutCallChargesUrlCy.map(URL("learnAboutCallChargesUrlCy", _)),
        statePensionAgeUrl.map(URL("statePensionAgeUrl", _)),
        tcNationalInsuranceRatesLettersUrl.map(URL("tcNationalInsuranceRatesLettersUrl", _)),
        tcNationalInsuranceRatesLettersUrlCy.map(URL("tcNationalInsuranceRatesLettersUrlCy", _)),
        tcPersonalAllowanceUrl.map(URL("tcPersonalAllowanceUrl", _)),
        tcPersonalAllowanceUrlCy.map(URL("tcPersonalAllowanceUrlCy", _)),
        scottishIncomeTaxUrl.map(URL("scottishIncomeTaxUrl", _)),
        scottishIncomeTaxUrlCy.map(URL("scottishIncomeTaxUrlCy", _)),
        cbTaxChargeUrl.map(URL("cbTaxChargeUrl", _)),
        cbTaxChargeUrlCy.map(URL("cbTaxChargeUrlCy", _)),
        selfAssessmentHelpAppealingPenaltiesUrl.map(URL("selfAssessmentHelpAppealingPenaltiesUrl", _)),
        selfAssessmentHelpAppealingPenaltiesUrlCy.map(URL("selfAssessmentHelpAppealingPenaltiesUrlCy", _)),
        addMissingTaxableIncomeUrl.map(URL("addMissingTaxableIncomeUrl", _)),
        helpToSaveGeneralEnquiriesUrl.map(URL("helpToSaveGeneralEnquiriesUrl", _)),
        helpToSaveGeneralEnquiriesUrlCy.map(URL("helpToSaveGeneralEnquiriesUrlCy", _)),
        helpToSaveDigitalAssistantUrl.map(URL("helpToSaveDigitalAssistantUrl", _)),
        selfAssessmentGeneralEnquiriesUrl.map(URL("selfAssessmentGeneralEnquiriesUrl", _)),
        selfAssessmentGeneralEnquiriesUrlCy.map(URL("selfAssessmentGeneralEnquiriesUrlCy", _)),
        simpleAssessmentGeneralEnquiriesUrl.map(URL("simpleAssessmentGeneralEnquiriesUrl", _)),
        simpleAssessmentGeneralEnquiriesUrlCy.map(URL("simpleAssessmentGeneralEnquiriesUrlCy", _)),
        cbGeneralEnquiriesUrl.map(URL("cbGeneralEnquiriesUrl", _)),
        cbGeneralEnquiriesUrlCy.map(URL("cbGeneralEnquiriesUrlCy", _)),
        taxCreditsGeneralEnquiriesUrl.map(URL("taxCreditsGeneralEnquiriesUrl", _)),
        taxCreditsGeneralEnquiriesUrlCy.map(URL("taxCreditsGeneralEnquiriesUrlCy", _)),
        otherTaxesGeneralEnquiriesUrl.map(URL("otherTaxesGeneralEnquiriesUrl", _)),
        otherTaxesGeneralEnquiriesUrlCy.map(URL("otherTaxesGeneralEnquiriesUrlCy", _)),
        findRepaymentPlanUrl.map(URL("findRepaymentPlanUrl", _)),
        findRepaymentPlanUrlCy.map(URL("findRepaymentPlanUrlCy", _)),
        pensionAnnualAllowanceUrl.map(URL("pensionAnnualAllowanceUrl", _)),
        pensionAnnualAllowanceUrlCy.map(URL("pensionAnnualAllowanceUrlCy", _)),
        childBenefitDigitalAssistantUrl.map(URL("childBenefitDigitalAssistantUrl", _)),
        childBenefitDigitalAssistantUrlCy.map(URL("childBenefitDigitalAssistantUrlCy", _)),
        incomeTaxDigitalAssistantUrl.map(URL("incomeTaxDigitalAssistantUrl", _)),
        incomeTaxDigitalAssistantUrlCy.map(URL("incomeTaxDigitalAssistantUrlCy", _)),
        selfAssessmentDigitalAssistantUrl.map(URL("selfAssessmentDigitalAssistantUrl", _)),
        selfAssessmentDigitalAssistantUrlCy.map(URL("selfAssessmentDigitalAssistantUrlCy", _)),
        taxCreditsDigitalAssistantUrl.map(URL("taxCreditsDigitalAssistantUrl", _)),
        taxCreditsDigitalAssistantUrlCy.map(URL("taxCreditsDigitalAssistantUrlCy", _)),
        tcStateBenefitsUrl.map(URL("tcStateBenefitsUrl", _)),
        tcStateBenefitsUrlCy.map(URL("tcStateBenefitsUrlCy", _)),
        tcCompanyBenefitsUrl.map(URL("tcCompanyBenefitsUrl", _)),
        tcCompanyBenefitsUrlCy.map(URL("tcCompanyBenefitsUrlCy", _)),
        niAppleWalletUrl.map(URL("niAppleWalletUrl", _)),
        niGoogleWalletUrl.map(URL("niGoogleWalletUrl", _))
      ).filter(_.isDefined)
    )

  private def callService(name: String)(f: => F[Option[JsValue]]): F[JsObject] =
    // If the service call returns a valid result or an error then map it into the object against
    // the supplied name, but if the result is None then just return an empty object so that the
    // services section will not appear in the final result at all.
    f.map {
      case Some(json) => obj(name -> json)
      case None       => obj()
    }

  private def mhtsStartup(implicit hc: HeaderCarrier): F[Option[JsValue]] =
    connector
      .doGet("mobile-help-to-save", "/mobile-help-to-save/startup", hc)
      .map(_.some)
      .recover {
        case NonFatal(e) =>
          logger.warn(
            s"""Exception thrown by "/mobile-help-to-save/startup", not returning any helpToSave result: ${e.getMessage}"""
          )
          None
      }

  private def tcrStartup(journeyId: JourneyId)(implicit hc: HeaderCarrier): F[Option[JsValue]] =
    connector
      .doGet("mobile-tax-credits-renewal",
             s"/income/tax-credits/submission/state/enabled?journeyId=${journeyId.value}",
             hc)
      .map[Option[JsValue]](res => obj("submissionsState" -> JsString((res \ "submissionsState").as[String])).some)
      .recover {
        case NonFatal(e) =>
          logger.warn(
            s"${journeyId.value} - Failed to retrieve TaxCreditsRenewals and exception is ${e.getMessage}! Default of submissionsState is error!"
          )
          obj("submissionsState" -> JsString("error")).some
      }

  private def inAppMsgsStartup(implicit hc: HeaderCarrier): F[Option[JsValue]] =
    connector
      .doGet("mobile-in-app-messages", "/in-app-messages", hc)
      .map(_.some)
      .recover {
        case NonFatal(e) =>
          logger.warn(
            s"""Exception thrown by "/mobile-in-app-messages/in-app-messages", not returning any inAppMessages result: ${e.getMessage}"""
          )
          Some(Json.parse("""{
                            |  "paye": [],
                            |  "tc": [],
                            |  "hts": [],
                            |  "tcp": []
                            |}
                            |""".stripMargin))
      }

  private def citizenDetailsStartup(
    nino:        String,
    shuttered:   Boolean
  )(implicit hc: HeaderCarrier
  ): F[Option[JsValue]] =
    if (shuttered) F.pure[Option[JsValue]](None)
    else
      connector
        .doGet("citizen-details", s"/citizen-details/$nino/designatory-details", hc)
        .map { p =>
          val person = p.as[PersonDetails]
          Option(
            Json.toJson(
              new JsObject(
                Map("name" -> Json.toJson(person.person.shortName), "address" -> Json.toJson(person.address))
              )
            )
          )
        }
        .recover {
          case e: UpstreamErrorResponse if e.statusCode == LOCKED =>
            logger.info("Person details are hidden")
            None
          case e: NotFoundException =>
            logger.info(s"No details found for nino '$nino'")
            None
          case _ =>
            logger.info(s"CID call failed for nino '$nino'")
            None
        }

  private def childBenefitStartup(childBenefitShutteredStatus: Shuttering): JsObject =
    obj(
      "childBenefit" -> obj("shuttering" -> childBenefitShutteredStatus)
    )

}
