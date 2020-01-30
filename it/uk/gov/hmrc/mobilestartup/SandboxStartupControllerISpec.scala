package uk.gov.hmrc.mobilestartup

import play.api.libs.ws.WSClient
import uk.gov.hmrc.mobilestartup.support.BaseISpec

class SandboxStartupControllerISpec extends BaseISpec {

  val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "This integration test" should {
    "start services via smserver" in {

      implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]

      val response = await(wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375").addHttpHeaders(mobileHeader).get)
      response.status shouldBe 200

    }
    "return 400 if journeyId not supplied" in {

      implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]

      val response = await(wsUrl("/startup").addHttpHeaders(mobileHeader).get)
      response.status shouldBe 400

    }

    "return 400 if journeyId is invalid" in {
      implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]
      val response = await(wsUrl(s"/startup?journeyId=ThisIsAnInvalidJourneyId").addHttpHeaders(mobileHeader).get)
      response.status shouldBe 400
    }
  }
}
