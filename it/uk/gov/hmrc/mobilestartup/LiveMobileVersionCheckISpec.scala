package uk.gov.hmrc.mobilestartup

import play.api.libs.json.Json.toJson
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.mobilestartup.domain.DeviceVersion
import uk.gov.hmrc.mobilestartup.domain.NativeOS.{Android, iOS}
import uk.gov.hmrc.mobilestartup.support.BaseISpec

class LiveMobileVersionCheckISpec extends BaseISpec {
  "GET /ping/ping" should {
    def request: WSRequest = wsUrl(s"/ping/ping").addHttpHeaders(acceptJsonHeader)

    "respond with 200" in {
      val response = request.get().futureValue
      response.status shouldBe 200
    }
  }

  "POST /mobile-startup/version-check" should {
    def request: WSRequest = wsUrl(s"/mobile-startup/version-check")

    "indicate that an upgrade is required for a version below the lower bound version of iOS" in {
      val response = request.addHttpHeaders(acceptJsonHeader).post(toJson(DeviceVersion(iOS, "3.0.6"))).futureValue

      response.status                                 shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean] shouldBe true
    }

    "indicate that an upgrade is not required for a version equal to the lower bound version of iOS" in {
      val response = request.addHttpHeaders(acceptJsonHeader).post(toJson(DeviceVersion(iOS, "3.0.7"))).futureValue

      response.status                                 shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean] shouldBe false
    }

    "indicate that an upgrade is not required for a version above the lower bound version of iOS" in {
      val response = request.addHttpHeaders(acceptJsonHeader).post(toJson(DeviceVersion(iOS, "3.0.8"))).futureValue

      response.status                                 shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean] shouldBe false
    }

    "indicate that an upgrade is required for a version below the lower bound version of android" in {
      val response = request.addHttpHeaders(acceptJsonHeader).post(toJson(DeviceVersion(Android, "5.0.21"))).futureValue

      response.status                                 shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean] shouldBe true
    }

    "indicate that an upgrade is not required for a version equal to the lower bound version of android" in {
      val response = request.addHttpHeaders(acceptJsonHeader).post(toJson(DeviceVersion(Android, "5.0.22"))).futureValue

      response.status                                 shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean] shouldBe false
    }

    "indicate that an upgrade is not required for a version above the lower bound version of android" in {
      val response = request.addHttpHeaders(acceptJsonHeader).post(toJson(DeviceVersion(Android, "5.0.23"))).futureValue

      response.status                                 shouldBe 200
      (response.json \ "upgradeRequired").as[Boolean] shouldBe false
    }
  }
}
