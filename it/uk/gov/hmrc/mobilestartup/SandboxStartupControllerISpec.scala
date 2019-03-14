package uk.gov.hmrc.mobilestartup

import play.api.libs.ws.WSClient
import uk.gov.hmrc.mobilestartup.support.BaseISpec

class SandboxStartupControllerISpec extends BaseISpec {

  val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "This integration test" should {
    "start services via smserver" in {

      implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]

      val response = await(wsUrl("/startup").addHttpHeaders(mobileHeader).get)
      response.status shouldBe 200

    }
  }
}
