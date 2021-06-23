/*
 * Copyright 2021 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object AuthStub {

  private val authUrl: String = "/auth/authorise"

  private val authoriseRequestBody: String = {
    """
      |{
      | "authorise": [ {
      |  "confidenceLevel" : 200
      |  } ],
      | "retrieve": ["nino"]
      |}""".stripMargin
  }

  private val accountsRequestJson: String = {
    """{ "authorise": [], "retrieve": ["nino","saUtr","optionalCredentials","confidenceLevel","optionalItmpName","allEnrolments","optionalName"] }""".stripMargin
  }

  private def loggedInResponse(
    nino:        String,
    saUtr:       String,
    activateUtr: Boolean
  ): String =
    s"""
       |{
       |  "nino": "$nino",
       |  "saUtr": "$saUtr",
       |  "optionalCredentials": {
       |    "providerId": "test-cred-id",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "allEnrolments": [{
       |      "key": "IR-SA",
       |      "identifiers": [{
       |        "key": "UTR",
       |        "value": "$saUtr"
       |      }],
       |      "state": "${if (activateUtr) "Activated" else "Deactivated"}"
       |}],
       |  "groupIdentifier": "groupId",
       |  "confidenceLevel": 200,
       |  "optionalItmpName": {
       |    "givenName": "Test",
       |    "familyName": "User"
       |  },
       |  "optionalName": {
       |    "name": "TestUser2"
       |  }
       |}
           """.stripMargin

  private def loggedInResponseNoNino(
                                saUtr:       String,
                                activateUtr: Boolean
                              ): String =
    s"""
       |{
       |  "saUtr": "$saUtr",
       |  "optionalCredentials": {
       |    "providerId": "test-cred-id",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "allEnrolments": [{
       |      "key": "IR-SA",
       |      "identifiers": [{
       |        "key": "UTR",
       |        "value": "$saUtr"
       |      }],
       |      "state": "${if (activateUtr) "Activated" else "Deactivated"}"
       |}],
       |  "groupIdentifier": "groupId",
       |  "confidenceLevel": 200,
       |  "optionalItmpName": {
       |    "givenName": "Test",
       |    "familyName": "User"
       |  },
       |  "optionalName": {
       |    "name": "TestUser2"
       |  }
       |}
           """.stripMargin

  private def loggedInResponseNoItmpName(
    nino:        String,
    saUtr:       String,
    activateUtr: Boolean
  ): String =
    s"""
       |{
       |  "nino": "$nino",
       |  "saUtr": "$saUtr",
       |  "optionalCredentials": {
       |    "providerId": "test-cred-id",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "allEnrolments": [{
       |      "key": "IR-SA",
       |      "identifiers": [{
       |        "key": "UTR",
       |        "value": "$saUtr"
       |      }],
       |      "state": "${if (activateUtr) "Activated" else "Deactivated"}"
       |}],
       |  "groupIdentifier": "groupId",
       |  "confidenceLevel": 200,
       |  "optionalName": {
       |    "name": "TestUser2"
       |  }
       |}
           """.stripMargin

  private def loggedInResponseNoNames(
    nino:        String,
    saUtr:       String,
    activateUtr: Boolean
  ): String =
    s"""
       |{
       |  "nino": "$nino",
       |  "saUtr": "$saUtr",
       |  "optionalCredentials": {
       |    "providerId": "test-cred-id",
       |    "providerType": "GovernmentGateway"
       |  },
       |  "allEnrolments": [{
       |      "key": "IR-SA",
       |      "identifiers": [{
       |        "key": "UTR",
       |        "value": "$saUtr"
       |      }],
       |      "state": "${if (activateUtr) "Activated" else "Deactivated"}"
       |}],
       |  "groupIdentifier": "groupId",
       |  "confidenceLevel": 200
       |}
           """.stripMargin

  def accountsFound(
    nino:        String  = "AA000006C",
    saUtr:       String  = "123456789",
    activateUtr: Boolean = true
  ): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(loggedInResponse(nino, saUtr, activateUtr))
        )
    )

  def accountsFoundMissingItmpName(
    nino:             String  = "AA000006C",
    saUtr:            String  = "123456789",
    activateUtr:      Boolean = true,
    bothNamesMissing: Boolean = false
  ): StubMapping = {
    val response =
      if (bothNamesMissing) loggedInResponseNoNames(nino, saUtr, activateUtr)
      else loggedInResponseNoItmpName(nino, saUtr, activateUtr)
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(response)
        )
    )
  }

  def userLoggedIn(
    nino:        String  = "AA000006C",
    saUtr:       String  = "123456789",
    activateUtr: Boolean = true
  ): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authoriseRequestBody, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(loggedInResponse(nino, saUtr, activateUtr))
        )
    )

  def userLoggedInNoNino(
                    saUtr:       String  = "123456789",
                    activateUtr: Boolean = true
                  ): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authoriseRequestBody, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(loggedInResponseNoNino(saUtr, activateUtr))
        )
    )

  def userIsLoggedInWithInsufficientConfidenceLevel()(implicit wireMockServer: WireMockServer): StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .withRequestBody(equalToJson(authoriseRequestBody))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", """MDTP detail="InsufficientConfidenceLevel"""")
        )
    )

  def userIsNotLoggedIn()(implicit wireMockServer: WireMockServer): StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .withRequestBody(equalToJson(authoriseRequestBody))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", """MDTP detail="MissingBearerToken"""")
        )
    )

  def authoriseShouldNotHaveBeenCalled()(implicit wireMockServer: WireMockServer): Unit =
    wireMockServer.verify(0, postRequestedFor(urlPathEqualTo("/auth/authorise")))
}
