/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, getRequestedFor, stubFor, urlPathEqualTo, urlPathMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object EnrolmentStoreStub {

  def principalIdsForUtrAre(
    utr:              String,
    principalUserIds: String
  ): StubMapping =
    stubFor(
      get(s"/enrolment-store-proxy/enrolment-store/enrolments/IR-SA~UTR~$utr/users?type=principal")
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(principalUserIds)
        )
    )

  def enrolmentStoreReturnErrorResponse(
    utr:                String,
    responseStatusCode: Int
  ): StubMapping =
    stubFor(
      get(s"/enrolment-store-proxy/enrolment-store/enrolments/IR-SA~UTR~$utr/users?type=principal")
        .willReturn(
          aResponse()
            .withStatus(responseStatusCode)
        )
    )

  def enrolmentStoreShouldNotHaveBeenCalled()(implicit wireMockServer: WireMockServer): Unit =
    wireMockServer.verify(
      0,
      getRequestedFor(
        urlPathEqualTo("/enrolment-store-proxy/enrolment-store/")
      )
    )

}
