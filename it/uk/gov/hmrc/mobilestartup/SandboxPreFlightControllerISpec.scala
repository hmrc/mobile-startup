package uk.gov.hmrc.mobilestartup

import play.api.http.HeaderNames
import uk.gov.hmrc.mobilestartup.support.BaseISpec

class SandboxPreFlightControllerISpec extends BaseISpec {

  private val headerThatSucceeds =
    Seq(HeaderNames.ACCEPT -> "application/vnd.hmrc.1.0+json", "X-MOBILE-USER-ID" -> "208606423740")

  private val journeyId = "f7a5d556-9f34-47cb-9d84-7e904f2fe704"

  private def withSandboxControl(value: String) = Seq("SANDBOX-CONTROL" -> value)

  def withJourneyParam(journeyId: String) = s"journeyId=$journeyId"

  val nino = "CS700100A"

  "POST of /preflight-check with X-MOBILE-USER-ID header" should {

    "successfully switch to the sandbox preflight" in {
      val response = await(wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}").addHttpHeaders(headerThatSucceeds: _*).get)

      response.status                           shouldBe 200
      (response.json \ "nino").as[String]       shouldBe nino
      (response.json \ "routeToIV").as[Boolean] shouldBe false
    }

    "return routeToIV = true when SANDBOX-CONTROL header = ROUTE-TO-IV" in {
      val response = await(
        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ROUTE-TO-IV"): _*)
          .get)

      response.status                           shouldBe 200
      (response.json \ "nino").as[String]       shouldBe nino
      (response.json \ "routeToIV").as[Boolean] shouldBe true
    }

    "return unauthorized when SANDBOX-CONTROL header = ERROR-401" in {
      val response = await(
        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-401"): _*)
          .get)

      response.status shouldBe 401
    }

    "return unauthorized when SANDBOX-CONTROL header = ERROR-403" in {
      val response = await(
        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-403"): _*)
          .get)

      response.status shouldBe 403
    }

    "return unauthorized when SANDBOX-CONTROL header = ERROR-500" in {
      val response = await(
        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-500"): _*)
          .get())

      response.status shouldBe 500
    }
  }
}
