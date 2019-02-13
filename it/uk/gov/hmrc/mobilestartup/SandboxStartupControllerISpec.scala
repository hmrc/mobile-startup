package uk.gov.hmrc.mobilestartup

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.ws.WSClient
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.integration.ServiceSpec

class SandboxStartupControllerISpec extends WordSpec with Matchers with ServiceSpec with WsScalaTestClient {

  def externalServices: Seq[String] = Seq("datastream", "auth")

  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))
  val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"



  "This integration test" should {
    "start services via smserver" in {

      implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]

      val response = wsUrl("/mobile-startup/startup").addHttpHeaders(mobileHeader).get.futureValue
      response.status shouldBe 200

    }
  }
}
