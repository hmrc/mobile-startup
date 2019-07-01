package uk.gov.hmrc.mobilestartup

import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import uk.gov.hmrc.mobilestartup.controllers.DeviceVersion
import uk.gov.hmrc.mobilestartup.support.BaseISpec

class SandboxPreflightControllerISpec extends BaseISpec {

  private val headerThatSucceeds =
    Seq(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON, HeaderNames.ACCEPT -> "application/vnd.hmrc.1.0+json", "X-MOBILE-USER-ID" -> "208606423740")

  private val journeyId = "f7a5d556-9f34-47cb-9d84-7e904f2fe704"

  private def withSandboxControl(value: String) = Seq("SANDBOX-CONTROL" -> value)

  def withJourneyParam(journeyId: String) = s"journeyId=$journeyId"

  private val versionPostRequest = Json.toJson(DeviceVersion("ios", "0.1.0"))

  val nino = "CS700100A"

  "POST of /preflight-check with X-MOBILE-USER-ID header" should {

    "successfully switch to the sandbox preflight" in {
      val response = await(wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}").addHttpHeaders(headerThatSucceeds: _*).post(versionPostRequest))

      response.status                                        shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean]        shouldBe false
      (response.json \ "accounts" \ "nino").as[String]       shouldBe nino
      (response.json \ "accounts" \ "routeToIV").as[Boolean] shouldBe false
    }

    "return upgradeRequired = true when SANDBOX-CONTROL header = UPGRADE-REQUIRED" in {
      val request = wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
        .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("UPGRADE-REQUIRED"): _*)
        .post(versionPostRequest)

      val response = await(request)

      response.status                                        shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean]        shouldBe true
      (response.json \ "accounts" \ "nino").as[String]       shouldBe nino
      (response.json \ "accounts" \ "routeToIV").as[Boolean] shouldBe false
    }

    "return routeToIv = true when SANDBOX-CONTROL header = ROUTE-TO-IV" in {
      val response = await(
        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ROUTE-TO-IV"): _*)
          .post(versionPostRequest))
      response.status                                               shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean]               shouldBe false
      (response.json \ "accounts" \ "nino").as[String]              shouldBe nino
      (response.json \ "accounts" \ "routeToIV").as[Boolean]        shouldBe true
    }



    "return unauthorized when SANDBOX-CONTROL header = ERROR-401" in {
      val response = await(
        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-401"): _*)
          .post(versionPostRequest))
      response.status shouldBe 401
    }

    "return unauthorized when SANDBOX-CONTROL header = ERROR-403" in {
      val response = await(
        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-403"): _*)
          .post(versionPostRequest))
      response.status shouldBe 403
    }

    "return unauthorized when SANDBOX-CONTROL header = ERROR-500" in {
      val response = await(
        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-500"): _*)
          .post(versionPostRequest))
      response.status shouldBe 500
    }
  }
}
