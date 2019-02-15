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

package uk.gov.hmrc.mobilestartup.services

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.domain.{DeviceVersion, ValidateAppVersion}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.service.Auditor

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VersionCheckService @Inject()(val configuration: Configuration, val auditConnector: AuditConnector) extends Auditor {

  def versionCheck(deviceVersion: DeviceVersion, journeyId: Option[String] = None)(
    implicit hc:                  HeaderCarrier,
    ex:                           ExecutionContext): Future[Boolean] =
    withAudit("upgradeRequired", Map("os" -> deviceVersion.os.toString)) {
      ValidateAppVersion.upgrade(deviceVersion)
    }

  override def appName: String = AppName.fromConfiguration(configuration)
}
