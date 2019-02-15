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

import net.ceedubs.ficus.readers.ValueReader

import scala.concurrent.Future

case class NativeVersion(ios: VersionRange, android: VersionRange)

trait LoadConfig {

  import com.typesafe.config.Config

  def config: Config
}

trait ApprovedAppVersions extends LoadConfig {

  import net.ceedubs.ficus.Ficus._

  private implicit val nativeVersionReader: ValueReader[NativeVersion] = ValueReader.relative { _ =>
    NativeVersion(
      VersionRange(config.as[String]("approvedAppVersions.ios")),
      VersionRange(config.as[String]("approvedAppVersions.android"))
    )
  }

  val appVersion: NativeVersion = config.as[NativeVersion]("approvedAppVersions")
}

trait ValidateAppVersion extends ApprovedAppVersions {
  import uk.gov.hmrc.mobilestartup.domain.NativeOS.{Android, iOS}

  def upgrade(deviceVersion: DeviceVersion): Future[Boolean] = {
    val outsideValidRange = deviceVersion.os match {
      case `iOS`   => appVersion.ios.excluded(Version(deviceVersion.version))
      case Android => appVersion.android.excluded(Version(deviceVersion.version))
    }
    Future.successful(outsideValidRange)
  }
}

object ValidateAppVersion extends ValidateAppVersion {

  import com.typesafe.config.{Config, ConfigFactory}

  lazy val config: Config = ConfigFactory.load()
}
