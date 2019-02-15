/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobilestartup.controllers

import java.util.UUID.randomUUID

import play.api.libs.json.JsValue
import play.api.libs.json.Json.{parse, toJson}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, _}
import uk.gov.hmrc.mobilestartup.domain.DeviceVersion
import uk.gov.hmrc.mobilestartup.domain.NativeOS.Android

import scala.concurrent.ExecutionContext.Implicits.global

class SandboxVersionCheckControllerSpec extends BaseControllerSpec {
  val controller = new SandboxVersionCheckController(stubControllerComponents())

  val sandboxControlHeader = "SANDBOX-CONTROL"
  val androidVersion       = DeviceVersion(Android, "0.1")

  val requestWithoutOS:               FakeRequest[JsValue] = FakeRequest().withBody(parse("""{ "version": "1.0" }""")).withHeaders(acceptJsonHeader)
  val requestWithoutVersion:          FakeRequest[JsValue] = FakeRequest().withBody(parse("""{ "os": "iOS" }""")).withHeaders(acceptJsonHeader)
  val androidRequestWithAcceptHeader: FakeRequest[JsValue] = FakeRequest().withBody(toJson(androidVersion)).withHeaders(acceptJsonHeader)

  "version check without SANDBOX-CONTROL header specified" should {
    "return upgradeRequired false when a journey id is supplied" in {
      val result = controller.versionCheck(Some(journeyId))(iOSRequestWithValidHeaders)
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeNotRequiredResult)
    }

    "return upgradeRequired false when no journey id is supplied" in {
      val result = controller.versionCheck(None)(iOSRequestWithValidHeaders)
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeNotRequiredResult)
    }

    "return upgradeRequired result for android OS" in {
      val result = controller.versionCheck(None)(androidRequestWithAcceptHeader)
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeNotRequiredResult)
    }

    "require the accept header" in {
      val result = controller.versionCheck(None)(iOSRequest)
      status(result) shouldBe 406
    }

    "require an app OS" in {
      val result = controller.versionCheck(None)(requestWithoutOS)
      status(result) shouldBe 400
    }

    "require a version" in {
      val result = controller.versionCheck(None)(requestWithoutVersion)
      status(result) shouldBe 400
    }
  }

  "version check with random SANDBOX-CONTROL header supplied" should {
    val sandboxHeader                            = sandboxControlHeader -> randomUUID().toString
    val iosRequestWithRandomSandboxControlHeader = iOSRequestWithValidHeaders.withHeaders(sandboxHeader)

    "return upgradeRequired false when a journey id is supplied" in {
      val result = controller.versionCheck(Some(journeyId))(iosRequestWithRandomSandboxControlHeader)
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeNotRequiredResult)
    }

    "return upgradeRequired false when no journey id is supplied" in {
      val result = controller.versionCheck(None)(iosRequestWithRandomSandboxControlHeader)
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeNotRequiredResult)
    }

    "return upgradeRequired result for android OS" in {
      val result = controller.versionCheck(None)(androidRequestWithAcceptHeader.withHeaders(sandboxHeader))
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeNotRequiredResult)
    }

    "require the accept header" in {
      val result = controller.versionCheck(None)(iOSRequest.withHeaders(sandboxHeader))
      status(result) shouldBe 406
    }

    "require an app OS" in {
      val result = controller.versionCheck(None)(requestWithoutOS.withHeaders(sandboxHeader))
      status(result) shouldBe 400
    }

    "require a version" in {
      val result = controller.versionCheck(None)(requestWithoutVersion.withHeaders(sandboxHeader))
      status(result) shouldBe 400
    }
  }

  "version check with 'ERROR-500' SANDBOX-CONTROL header supplied" should {
    val sandboxHeader                            = sandboxControlHeader -> "ERROR-500"
    val iosRequestWithRandomSandboxControlHeader = iOSRequestWithValidHeaders.withHeaders(sandboxHeader)

    "return 500 false when a journey id is supplied" in {
      val result = controller.versionCheck(Some(journeyId))(iosRequestWithRandomSandboxControlHeader)
      status(result) shouldBe 500
    }

    "return upgradeRequired false when no journey id is supplied" in {
      val result = controller.versionCheck(None)(iosRequestWithRandomSandboxControlHeader)
      status(result) shouldBe 500
    }

    "return upgradeRequired result for android OS" in {
      val result = controller.versionCheck(None)(androidRequestWithAcceptHeader.withHeaders(sandboxHeader))
      status(result) shouldBe 500
    }

    "require the accept header" in {
      val result = controller.versionCheck(None)(iOSRequest.withHeaders(sandboxHeader))
      status(result) shouldBe 406
    }

    "require an app OS" in {
      val result = controller.versionCheck(None)(requestWithoutOS.withHeaders(sandboxHeader))
      status(result) shouldBe 400
    }

    "require a version" in {
      val result = controller.versionCheck(None)(requestWithoutVersion.withHeaders(sandboxHeader))
      status(result) shouldBe 400
    }
  }

  "version check with 'UPGRADE-REQUIRED' SANDBOX-CONTROL header supplied" should {
    val sandboxHeader                            = sandboxControlHeader -> "UPGRADE-REQUIRED"
    val iosRequestWithRandomSandboxControlHeader = iOSRequestWithValidHeaders.withHeaders(sandboxHeader)

    "return upgradeRequired false when a journey id is supplied" in {
      val result = controller.versionCheck(Some(journeyId))(iosRequestWithRandomSandboxControlHeader)
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeRequiredResult)
    }

    "return upgradeRequired false when no journey id is supplied" in {
      val result = controller.versionCheck(None)(iosRequestWithRandomSandboxControlHeader)
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeRequiredResult)
    }

    "return upgradeRequired result for android OS" in {
      val result = controller.versionCheck(None)(androidRequestWithAcceptHeader.withHeaders(sandboxHeader))
      status(result)        shouldBe 200
      contentAsJson(result) shouldBe parse(upgradeRequiredResult)
    }

    "require the accept header" in {
      val result = controller.versionCheck(None)(iOSRequest.withHeaders(sandboxHeader))
      status(result) shouldBe 406
    }

    "require an app OS" in {
      val result = controller.versionCheck(None)(requestWithoutOS.withHeaders(sandboxHeader))
      status(result) shouldBe 400
    }

    "require a version" in {
      val result = controller.versionCheck(None)(requestWithoutVersion.withHeaders(sandboxHeader))
      status(result) shouldBe 400
    }
  }
}
