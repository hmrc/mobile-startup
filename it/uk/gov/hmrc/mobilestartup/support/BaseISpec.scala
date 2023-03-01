/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.mobilestartup.support

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.OptionValues
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class BaseISpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout {

  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader:        (String, String) = "Accept"        -> "application/vnd.hmrc.1.0+json"
  protected val authorizationJsonHeader: (String, String) = "AUTHORIZATION" -> "Bearer test"

  def config: Map[String, Any] =
    Map(
      "auditing.enabled"                                      -> false,
      "microservice.services.auth.port"                       -> wireMockPort,
      "microservice.services.datastream.port"                 -> wireMockPort,
      "microservice.services.mobile-tax-credits-renewal.port" -> wireMockPort,
      "microservice.services.citizen-details.port"            -> wireMockPort,
      "microservice.services.enrolment-store-proxy.port"      -> wireMockPort,
      "auditing.consumer.baseUri.port"                        -> wireMockPort,
      "feature.userPanelSignUp"                               -> true,
      "feature.enablePushNotificationTokenRegistration"       -> true,
      "feature.helpToSave.enableBadge"                        -> true,
      "feature.paperlessAlertDialogs"                         -> true,
      "feature.paperlessAdverts"                              -> true,
      "feature.htsAdverts"                                    -> true,
      "feature.saTile"                                        -> true,
      "feature.annualTaxSummaryLink"                          -> true,
      "enableMultipleGGIDCheck"                               -> true,
      "url.cbProofOfEntitlementUrl"                           -> "/child-benefit/view-proof-entitlement",
      "url.cbProofOfEntitlementUrlCy"                         -> "/child-benefit/view-proof-entitlementCy",
      "url.cbPaymentHistoryUrl"                               -> "/child-benefit/view-payment-history",
      "url.cbPaymentHistoryUrlCy"                             -> "/child-benefit/view-payment-historyCy"
    )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
