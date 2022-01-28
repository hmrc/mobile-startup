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

package uk.gov.hmrc.mobilestartup.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object CitizenDetailsStub {

  def cidPersonForNinoAre(cidPerson: String): StubMapping =
    stubFor(
      get("/citizen-details/nino/AA000006C")
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(cidPerson)
        )
    )

  def cidWillReturnErrorResponse(responseStatusCode: Int): StubMapping =
    stubFor(
      get("/citizen-details/nino/AA000006C")
        .willReturn(
          aResponse()
            .withStatus(responseStatusCode)
        )
    )

}
