package uk.gov.hmrc.mobilestartup

import play.api.http.HeaderNames
import play.api.libs.ws.WSClient
import uk.gov.hmrc.mobilestartup.support.BaseISpec

class SandboxStartupControllerISpec extends BaseISpec {

  val mobileHeader = Seq(HeaderNames.ACCEPT -> "application/vnd.hmrc.1.0+json", "X-MOBILE-USER-ID" -> "208606423740")

  private def withSandboxControl(value: String) = Seq("SANDBOX-CONTROL" -> value)

  override implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "This integration test" should {
    "start services via smserver" in {
      val response =
        await(wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375").addHttpHeaders(mobileHeader: _*).get)
      response.status shouldBe 200
    }

    "return 200 and RENEWALS OPEN json where SANDBOX-CONTROL is RENEWALS-OPEN" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("RENEWALS-OPEN"): _*)
          .get()
      )
      response.status                                                       shouldBe 200
      (response.json \ "taxCreditRenewals" \ "submissionsState").as[String] shouldBe "open"
    }

    "return 200 and RENEWALS VIEW ONLY json where SANDBOX-CONTROL is RENEWALS-VIEW-ONLY" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("RENEWALS-VIEW-ONLY"): _*)
          .get()
      )
      response.status                                                       shouldBe 200
      (response.json \ "taxCreditRenewals" \ "submissionsState").as[String] shouldBe "status_view_only"
    }

    "return 200 and RENEWALS CLOSE json where SANDBOX-CONTROL is RENEWALS-CLOSED" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("RENEWALS-CLOSED"): _*)
          .get()
      )
      response.status                                                       shouldBe 200
      (response.json \ "taxCreditRenewals" \ "submissionsState").as[String] shouldBe "closed"
    }

    "return 200 and HTS ENROLLED json where SANDBOX-CONTROL is HTS-ENROLLED" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("HTS-ENROLLED"): _*)
          .get()
      )
      response.status                                                       shouldBe 200
      (response.json \ "helpToSave" \ "user" \ "state").as[String] shouldBe "Enrolled"
    }

    "return 200 and HTS ELIGIBLE json where SANDBOX-CONTROL is HTS-ELIGIBLE" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("HTS-ELIGIBLE"): _*)
          .get()
      )
      response.status                                                       shouldBe 200
      (response.json \ "helpToSave" \ "user" \ "state").as[String] shouldBe "NotEnrolledButEligible"
    }

    "return 200 and HTS NOT ENROLLED json where SANDBOX-CONTROL is HTS-NOT-ENROLLED" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("HTS-NOT-ENROLLED"): _*)
          .get()
      )
      response.status                                                       shouldBe 200
      (response.json \ "helpToSave" \ "user" \ "state").as[String] shouldBe "NotEnrolled"
    }

    "return 401 if unauthenticated where SANDBOX-CONTROL is ERROR-401" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("ERROR-401"): _*)
          .get()
      )
      response.status shouldBe 401
    }

    "return 403 if forbidden where SANDBOX-CONTROL is ERROR-403" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("ERROR-403"): _*)
          .get()
      )
      response.status shouldBe 403
    }

    "return 500 if there is an error where SANDBOX-CONTROL is ERROR-500" in {
      val response = await(
        wsUrl("/startup?journeyId=7f1b5289-5f4d-4150-93a3-ff02dda28375")
          .addHttpHeaders(mobileHeader ++ withSandboxControl("ERROR-500"): _*)
          .get()
      )
      response.status shouldBe 500
    }

    "return 400 if journeyId not supplied" in {
      val response = await(wsUrl("/startup").addHttpHeaders(mobileHeader: _*).get)
      response.status shouldBe 400
    }

    "return 400 if journeyId is invalid" in {
      val response = await(wsUrl(s"/startup?journeyId=ThisIsAnInvalidJourneyId").addHttpHeaders(mobileHeader: _*).get)
      response.status shouldBe 400
    }
  }
}
