package utils

import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient

trait BaseISpec extends WordSpecLike with Matchers with WsScalaTestClient with GuiceOneServerPerSuite with WireMockSupport {
  override implicit lazy val app: Application = appBuilder.build()

  def config: Map[String, Any] = Map(
    "microservice.services.service-locator.enabled" -> false,
    "auditing.enabled"                              -> false,
    // These are the services we need to support testing
    "microservice.services.auth.host"            -> wireMockHost,
    "microservice.services.auth.port"            -> wireMockPort,
    "microservice.services.service-locator.host" -> wireMockHost,
    "microservice.services.service-locator.port" -> wireMockPort,
    "auditing.consumer.baseUri.host"             -> wireMockHost,
    "auditing.consumer.baseUri.port"             -> wireMockPort,
    // The following are the services we call to aggregate the startup data
    "microservice.services.personal-income.host"            -> wireMockHost,
    "microservice.services.personal-income.port"            -> wireMockPort,
    "microservice.services.mobile-help-to-save.host"        -> wireMockHost,
    "microservice.services.mobile-help-to-save.port"        -> wireMockPort,
    "microservice.services.mobile-tax-credits-renewal.host" -> wireMockHost,
    "microservice.services.mobile-tax-credits-renewal.port" -> wireMockPort
  )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
