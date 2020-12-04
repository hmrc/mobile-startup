/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId

import scala.util.control.NonFatal

case class FeatureFlag(
  name:    String,
  enabled: Boolean)

object FeatureFlag {
  implicit val formats: Format[FeatureFlag] = Json.format[FeatureFlag]
}

/**
  * Decided to implement this generically using Tagless as an example of how it can be introduced
  * into a codebase without necessarily converting everything. It did require introducing a type parameter
  * onto the `GenericConnector` trait but that had very little impact beyond the change to the guice wiring.
  */
class StartupServiceImpl[F[_]] @Inject() (
  connector:                               GenericConnector[F],
  userPanelSignUp:                         Boolean,
  helpToSaveEnableBadge:                   Boolean,
  enablePushNotificationTokenRegistration: Boolean,
  enablePaperlessAlertDialogues:           Boolean,
  enablePaperlessAdverts:                  Boolean,
  enableHtsAdverts:                        Boolean
)(implicit F:                              MonadError[F, Throwable])
    extends StartupService[F] {

  override def startup(
    nino:        String,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier
  ): F[JsObject] =
    (callService("helpToSave")(mhtsStartup),
     callService("taxCreditRenewals")(tcrStartup(journeyId)),
     callService("messages")(inAppMsgsStartup),
     featureFlags.pure[F]).mapN((a, b, c, d) => a ++ b ++ c ++ d)

  private val featureFlags: JsObject =
    obj(
      "feature" -> List(
        FeatureFlag("userPanelSignUp", userPanelSignUp),
        FeatureFlag("helpToSaveEnableBadge", helpToSaveEnableBadge),
        FeatureFlag("enablePushNotificationTokenRegistration", enablePushNotificationTokenRegistration),
        FeatureFlag("paperlessAlertDialogues", enablePaperlessAlertDialogues),
        FeatureFlag("paperlessAdverts", enablePaperlessAdverts),
        FeatureFlag("htsAdverts", enableHtsAdverts)
      )
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
          Logger.warn(
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
          Logger.warn(
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
          Logger.warn(
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

}
