package uk.gov.hmrc.mobilestartup

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.ws.WSClient
import uk.gov.hmrc.integration.ServiceSpec

class ExampleIntegrationTest extends WordSpec with Matchers with ServiceSpec with WsScalaTestClient {

  def externalServices: Seq[String] = Seq("datastream", "auth")

  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))

  "This integration test" should {
    "start services via smserver" in {

      val wsClient = app.injector.instanceOf[WSClient]

      val response = wsClient.url(resource("/mobile-startup/startup")).get.futureValue
      response.status shouldBe 200

    }
  }
}
