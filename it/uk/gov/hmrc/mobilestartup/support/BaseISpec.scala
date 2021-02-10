package uk.gov.hmrc.mobilestartup.support

import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class BaseISpec
    extends WordSpecLike
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout {

  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader:        (String, String) = "Accept"        -> "application/vnd.hmrc.1.0+json"
  protected val authorizationJsonHeader: (String, String) = "Authorization" -> "Bearer test"

  def config: Map[String, Any] =
    Map(
      "auditing.enabled"                                      -> true,
      "microservice.services.auth.port"                       -> wireMockPort,
      "microservice.services.datastream.port"                 -> wireMockPort,
      "microservice.services.mobile-tax-credits-renewal.port" -> wireMockPort,
      "auditing.consumer.baseUri.port"                        -> wireMockPort,
      "feature.userPanelSignUp"                               -> true,
      "feature.enablePushNotificationTokenRegistration"       -> true,
      "feature.helpToSave.enableBadge"                        -> true,
      "feature.paperlessAlertDialogues"                       -> true,
      "feature.paperlessAlertDialogs"                         -> true,
      "feature.paperlessAdverts"                              -> true,
      "feature.htsAdverts"                                    -> true,
      "feature.annualTaxSummaryLink"                          -> true
    )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
