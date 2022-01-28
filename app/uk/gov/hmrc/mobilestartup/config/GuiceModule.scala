/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.mobilestartup.config

import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, TypeLiteral}
import javax.inject.Inject
import play.api.{Configuration, Environment, Logger, LoggerLike}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.CorePost
import uk.gov.hmrc.mobilestartup.connectors.{GenericConnector, GenericConnectorImpl}
import uk.gov.hmrc.mobilestartup.controllers.api.ApiAccess
import uk.gov.hmrc.mobilestartup.services.{LivePreFlightService, LiveStartupService, PreFlightService, StartupService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.collection.JavaConverters._
import scala.concurrent.Future

class GuiceModule @Inject() (
  environment:   Environment,
  configuration: Configuration)
    extends AbstractModule {

  val servicesConfig: ServicesConfig = new ServicesConfig(configuration)

  override def configure(): Unit = {

    bindConfigInt("controllers.confidenceLevel")
    bindConfigString("appUrl", "appUrl")
    bindConfigBoolean("feature.userPanelSignUp")
    bindConfigBoolean("feature.enablePushNotificationTokenRegistration")
    bindConfigBoolean("feature.paperlessAlertDialogs")
    bindConfigBoolean("feature.paperlessAdverts")
    bindConfigBoolean("feature.htsAdverts")
    bindConfigBoolean("feature.annualTaxSummaryLink")
    bind(classOf[Logger]).toInstance(Logger(this.getClass))

    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    bind(classOf[CorePost]).to(classOf[WSHttpImpl])

    bind(new TypeLiteral[GenericConnector[Future]] {}).to(classOf[GenericConnectorImpl])
    bind(new TypeLiteral[StartupService[Future]] {}).to(classOf[LiveStartupService])
    bind(new TypeLiteral[PreFlightService[Future]] {}).to(classOf[LivePreFlightService])

    bind(classOf[ApiAccess]).toInstance(
      ApiAccess("PRIVATE", configuration.underlying.getStringList("api.access.white-list.applicationIds").asScala)
    )
  }

  /**
    * Binds a configuration value using the `path` as the name for the binding.
    * Throws an exception if the configuration value does not exist or cannot be read as an Int.
    */
  private def bindConfigInt(path: String): Unit =
    bindConstant().annotatedWith(named(path)).to(configuration.underlying.getInt(path))

  private def bindConfigString(
    name: String,
    path: String
  ): Unit =
    bindConstant().annotatedWith(named(name)).to(configuration.underlying.getString(path))

  private def bindConfigBoolean(path: String): Unit =
    bindConstant().annotatedWith(named(path)).to(configuration.underlying.getBoolean(path))
}
