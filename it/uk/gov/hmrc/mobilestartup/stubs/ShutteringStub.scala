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

package uk.gov.hmrc.mobilestartup.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object ShutteringStub {

  def stubForShutteringDisabled(service: String): StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/$service/shuttered-status?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "shuttered": false,
                       |  "title":     "",
                       |  "message":    ""
                       |}
          """.stripMargin)
      )
    )

  def stubForShutteringEnabled(service: String): StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/$service/shuttered-status?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "shuttered": true,
                       |  "title":     "Shuttered",
                       |  "message":   "The service is currently not available"
                       |}
          """.stripMargin)
      )
    )

}
