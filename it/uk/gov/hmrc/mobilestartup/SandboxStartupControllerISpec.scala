package uk.gov.hmrc.mobilestartup

import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import utils.BaseISpec

class SandboxStartupControllerISpec extends BaseISpec with FutureAwaits with DefaultAwaitTimeout {

  val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "This integration test" should {
    "start services via smserver" in {

      implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]

      val response = await(wsUrl("/").addHttpHeaders(mobileHeader).get)
      response.status shouldBe 200

    }
  }
}
