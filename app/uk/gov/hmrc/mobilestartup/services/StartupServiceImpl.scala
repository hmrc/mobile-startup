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
import cats.MonadError
import cats.implicits._

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.PersonDetails
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import play.api.http.Status.LOCKED

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
class StartupServiceImpl[F[_]] @Inject() (
  connector:                                                     GenericConnector[F],
  userPanelSignUp:                                               Boolean,
  enablePushNotificationTokenRegistration:                       Boolean,
  enablePaperlessAlertDialogs:                                   Boolean,
  enablePaperlessAdverts:                                        Boolean,
  enableHtsAdverts:                                              Boolean,
  enableAnnualTaxSummaryLink:                                    Boolean,
  cbProofOfEntitlementUrl:                                       Option[String],
  cbProofOfEntitlementUrlCy:                                     Option[String],
  cbPaymentHistoryUrl:                                           Option[String],
  cbPaymentHistoryUrlCy:                                         Option[String],
  cbChangeBankAccountUrl:                                        Option[String],
  cbChangeBankAccountUrlCy:                                      Option[String],
  statePensionUrl:                                               Option[String],
  niSummaryUrl:                                                  Option[String],
  niContributionsUrl:                                            Option[String],
  enablePayeCustomerSatisfactionSurveyAdverts:                   Boolean,
  enableSelfAssessmentCustomerSatisfactionSurveyAdverts:         Boolean,
  enableSelfAssessmentPaymentsCustomerSatisfactionSurveyAdverts: Boolean,
  enableTaxCreditsCustomerSatisfactionSurveyAdverts:             Boolean,
  enableHelpToSaveCustomerSatisfactionSurveyAdverts:             Boolean,
  enableMessagesCustomerSatisfactionSurveyAdverts:               Boolean,
  enableFormTrackerCustomerSatisfactionSurveyAdverts:            Boolean,
  enableTaxCalculatorCustomerSatisfactionSurveyAdverts:          Boolean,
  enableYourDetailsCustomerSatisfactionSurveyAdverts:            Boolean
)(implicit F:                                                    MonadError[F, Throwable])
    extends StartupService[F] {

  val logger: Logger = Logger(this.getClass)

  override def startup(
    nino:        String,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier
  ): F[JsObject] =
    (callService("helpToSave")(mhtsStartup),
     callService("taxCreditRenewals")(tcrStartup(journeyId)),
     callService("messages")(inAppMsgsStartup),
     callService("user")(citizenDetailsStartup(nino)),
     featureFlags.pure[F],
     urls.pure[F]).mapN((a, b, c, d, e, f) => a ++ b ++ c ++ d ++ e ++ f)

  private val featureFlags: JsObject =
    obj(
      "feature" -> List(
        FeatureFlag("userPanelSignUp", userPanelSignUp),
        FeatureFlag("enablePushNotificationTokenRegistration", enablePushNotificationTokenRegistration),
        FeatureFlag("paperlessAlertDialogs", enablePaperlessAlertDialogs),
        FeatureFlag("paperlessAdverts", enablePaperlessAdverts),
        FeatureFlag("htsAdverts", enableHtsAdverts),
        FeatureFlag("annualTaxSummaryLink", enableAnnualTaxSummaryLink),
        FeatureFlag("payeCustomerSatisfactionSurveyAdverts", enablePayeCustomerSatisfactionSurveyAdverts),
        FeatureFlag("selfAssessmentCustomerSatisfactionSurveyAdverts",
                    enableSelfAssessmentCustomerSatisfactionSurveyAdverts),
        FeatureFlag("selfAssessmentPaymentsCustomerSatisfactionSurveyAdverts",
                    enableSelfAssessmentPaymentsCustomerSatisfactionSurveyAdverts),
        FeatureFlag("taxCreditsCustomerSatisfactionSurveyAdverts", enableTaxCreditsCustomerSatisfactionSurveyAdverts),
        FeatureFlag("helpToSaveCustomerSatisfactionSurveyAdverts", enableHelpToSaveCustomerSatisfactionSurveyAdverts),
        FeatureFlag("messagesCustomerSatisfactionSurveyAdverts", enableMessagesCustomerSatisfactionSurveyAdverts),
        FeatureFlag("formTrackerCustomerSatisfactionSurveyAdverts", enableFormTrackerCustomerSatisfactionSurveyAdverts),
        FeatureFlag("taxCalculatorCustomerSatisfactionSurveyAdverts",
                    enableTaxCalculatorCustomerSatisfactionSurveyAdverts),
        FeatureFlag("yourDetailsCustomerSatisfactionSurveyAdverts", enableYourDetailsCustomerSatisfactionSurveyAdverts)
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
        statePensionUrl.map(URL("statePensionUrl", _)),
        niSummaryUrl.map(URL("niSummaryUrl", _)),
        niContributionsUrl.map(URL("niContributionsUrl", _))
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

  private def citizenDetailsStartup(nino: String)(implicit hc: HeaderCarrier): F[Option[JsValue]] =
    connector
      .doGet("citizen-details", s"/citizen-details/$nino/designatory-details", hc)
      .map { p =>
        val person = p.as[PersonDetails]
        Option(
          Json.toJson(
            new JsObject(Map("name" -> Json.toJson(person.person.shortName), "address" -> Json.toJson(person.address)))
          )
        )
      }
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == LOCKED =>
          logger.info("Person details are hidden")
          None
        case e: NotFoundException =>
          logger.info(s"No details found for nino '$nino'")
          None
        case _ =>
          logger.info(s"CID call failed for nino '$nino'")
          None
      }

}
