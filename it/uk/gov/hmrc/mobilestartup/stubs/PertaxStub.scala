/*
 * Copyright 2026 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.mobilestartup.stubs.AuthStub.{accountsRequestJson, authUrl, loggedInResponse}

object PertaxStub {

  private val pertaxUrl: String = "/pertax/authorise"

  private def getPertaxResponse(
    code:    String,
    message: String
  ) =
    s"""
       |{
       |   "code":"$code",
       |   "message":"$message"
       |}
       |""".stripMargin

  def pertaxAuthorise(
    code:    String,
    message: String
  ): StubMapping =
    stubFor(
      post(urlEqualTo(pertaxUrl))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(getPertaxResponse(code, message))
        )
    )

  def pertaxUnAuthorise(status: Int): StubMapping =
    stubFor(
      post(urlEqualTo(pertaxUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody("")
        )
    )
}
