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

package uk.gov.hmrc.mobilestartup.domain

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import uk.gov.hmrc.mobilestartup.domain.NativeOS.{Android, iOS}

class ValidateAppVersionSpec extends WordSpecLike with Matchers with ScalaFutures {

  def validateAppVersion(iosVersionRange: String = "[0.0.1,)", androidVersionRange: String = "[0.0.1,)"): ValidateAppVersion =
    new ValidateAppVersion {
      override lazy val config: Config = ConfigFactory.parseString(s"""approvedAppVersions {
         |  ios = "$iosVersionRange"
         |  android = "$androidVersionRange"
         |}
         | """.stripMargin)
    }

  "Validating app version" should {
    "app version 1.2.0 valid" in {
      val deviceVersion = DeviceVersion(iOS, "1.2.0")
      validateAppVersion().upgrade(deviceVersion).futureValue shouldBe false
    }
    "app version 1.3.0 valid" in {
      val deviceVersion = DeviceVersion(Android, "1.3.0")
      validateAppVersion("[1.3.0,)").upgrade(deviceVersion).futureValue shouldBe false
    }

    "upgrade required for iOS app version 1.0.0 valid" in {
      val deviceVersion = DeviceVersion(iOS, "1.0.0")
      validateAppVersion("[1.2.0,1.3.0]").upgrade(deviceVersion).futureValue shouldBe true
    }
    "upgrade required for Android app version 1.0.0 valid" in {
      val deviceVersion = DeviceVersion(Android, "1.0.0")
      validateAppVersion(androidVersionRange = "[1.2.0,1.3.0]").upgrade(deviceVersion).futureValue shouldBe true
    }
  }
}