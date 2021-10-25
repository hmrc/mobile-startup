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

package uk.gov.hmrc.mobilestartup

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilestartup.support.BaseISpec
import uk.gov.hmrc.mobilestartup.stubs.AuthStub._
import uk.gov.hmrc.mobilestartup.stubs.AuditStub._
import uk.gov.hmrc.mobilestartup.stubs.CitizenDetailsStub._
import uk.gov.hmrc.mobilestartup.stubs.EnrolmentStoreStub._
import eu.timepit.refined.auto._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Future

trait LivePreFlightControllerTests extends BaseISpec {
  val nino:      Nino      = Nino("AA000006C")
  val saUtr:     SaUtr     = SaUtr("123456789")
  val journeyId: JourneyId = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val url:       String    = s"/preflight-check?journeyId=$journeyId"

  private val cidPersonJson = s"""{ 
                                 |  "name": {
                                 |    "current": {
                                 |      "firstName": "John",
                                 |      "lastName": "Smith"
                                 |    },
                                 |    "previous": []
                                 |  },
                                 |  "ids": {
                                 |    "nino": "AA000006C",
                                 |    "sautr": "123123123"
                                 |  },
                                 |  "dateOfBirth": "11121971"
                                 |}""".stripMargin

  private val principalIdsJson = s"""{
                                    |    "principalUserIds": [
                                    |       "ABCEDEFGI1234567",
                                    |       "ABCEDEFGI1234568"
                                    |    ]
                                    |}""".stripMargin

  private val noPrincipalIdsJson = s"""{
                                      |    "principalUserIds": []
                                      |}""".stripMargin

  def getRequestWithAcceptHeader(url: String): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader, authorizationJsonHeader).get()

  def postRequestWithAcceptHeader(
    url:  String,
    form: JsValue
  ): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader).post(form)

  def postRequestWithAcceptHeader(url: String): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader).post("")

  "GET /preflight-check" should {

    "return account details" in {
      accountsFound(nino.nino, saUtr.utr)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                          shouldBe 200
      (response.json \ "nino").as[String]                      shouldBe nino.nino
      (response.json \ "saUtr").as[String]                     shouldBe saUtr.utr
      (response.json \ "name").as[String]                      shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean]                shouldBe false
      (response.json \ "utr" \ "saUtr").as[String]             shouldBe "123456789"
      (response.json \ "utr" \ "status").as[String]            shouldBe "activated"
      (response.json \ "utr" \ "inactiveEnrolmentUrl").isEmpty shouldBe true

    }

    "return account details with name if itmpName not available" in {
      accountsFoundMissingItmpName(nino.nino, saUtr.utr)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                          shouldBe 200
      (response.json \ "nino").as[String]                      shouldBe nino.nino
      (response.json \ "saUtr").as[String]                     shouldBe saUtr.utr
      (response.json \ "name").as[String]                      shouldBe "TestUser2"
      (response.json \ "routeToIV").as[Boolean]                shouldBe false
      (response.json \ "utr" \ "saUtr").as[String]             shouldBe "123456789"
      (response.json \ "utr" \ "status").as[String]            shouldBe "activated"
      (response.json \ "utr" \ "inactiveEnrolmentUrl").isEmpty shouldBe true

    }

    "return account details with no name if itmpName and name not available" in {
      accountsFoundMissingItmpName(nino.nino, saUtr.utr, bothNamesMissing = true)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      response.status                                          shouldBe 200
      (response.json \ "nino").as[String]                      shouldBe nino.nino
      (response.json \ "saUtr").as[String]                     shouldBe saUtr.utr
      (response.json \ "name").isEmpty                         shouldBe true
      (response.json \ "routeToIV").as[Boolean]                shouldBe false
      (response.json \ "utr" \ "saUtr").as[String]             shouldBe "123456789"
      (response.json \ "utr" \ "status").as[String]            shouldBe "activated"
      (response.json \ "utr" \ "inactiveEnrolmentUrl").isEmpty shouldBe true

    }

    "Look for SaUtr on citizen-details if none returned from auth" in {
      accountsFoundMissingSaUtr(nino.nino)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody
      cidPersonForNinoAre(cidPersonJson)
      principalIdsForUtrAre("123123123", principalIdsJson)

      val response = await(getRequestWithAcceptHeader(url))

      response.status                               shouldBe 200
      (response.json \ "nino").as[String]           shouldBe nino.nino
      (response.json \ "name").as[String]           shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean]     shouldBe false
      (response.json \ "utr" \ "saUtr").as[String]  shouldBe "123123123"
      (response.json \ "utr" \ "status").as[String] shouldBe "wrongAccount"
      (response.json \ "utr" \ "inactiveEnrolmentUrl")
        .as[String] shouldBe "/personal-account/self-assessment"
    }

    "Return correct url if no principalIds found for CID utr on Enrolment store proxy" in {
      accountsFoundMissingSaUtr(nino.nino)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody
      cidPersonForNinoAre(cidPersonJson)
      principalIdsForUtrAre("123123123", noPrincipalIdsJson)

      val response = await(getRequestWithAcceptHeader(url))

      response.status                               shouldBe 200
      (response.json \ "nino").as[String]           shouldBe nino.nino
      (response.json \ "name").as[String]           shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean]     shouldBe false
      (response.json \ "utr" \ "saUtr").as[String]  shouldBe "123123123"
      (response.json \ "utr" \ "status").as[String] shouldBe "noEnrolment"
      (response.json \ "utr" \ "inactiveEnrolmentUrl")
        .as[String] shouldBe "/personal-account/sa-enrolment"
    }

    "return correct utr object if call to Enrolment Store returns 204" in {
      accountsFoundMissingSaUtr(nino.nino)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody
      cidPersonForNinoAre(cidPersonJson)
      enrolmentStoreReturnErrorResponse("123123123", 204)

      val response = await(getRequestWithAcceptHeader(url))

      response.status                               shouldBe 200
      (response.json \ "nino").as[String]           shouldBe nino.nino
      (response.json \ "name").as[String]           shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean]     shouldBe false
      (response.json \ "utr" \ "saUtr").as[String]  shouldBe "123123123"
      (response.json \ "utr" \ "status").as[String] shouldBe "noEnrolment"
      (response.json \ "utr" \ "inactiveEnrolmentUrl")
        .as[String] shouldBe "/personal-account/sa-enrolment"

    }

    "return no utr object if call to CID fails with 400" in {
      accountsFoundMissingSaUtr(nino.nino)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody
      cidWillReturnErrorResponse(400)

      val response = await(getRequestWithAcceptHeader(url))

      response.status                           shouldBe 200
      (response.json \ "nino").as[String]       shouldBe nino.nino
      (response.json \ "name").as[String]       shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean] shouldBe false
      (response.json \ "utr").isEmpty           shouldBe true

    }

    "return no utr status if call to CID fails with 404" in {
      accountsFoundMissingSaUtr(nino.nino)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody
      cidWillReturnErrorResponse(404)

      val response = await(getRequestWithAcceptHeader(url))

      response.status                               shouldBe 200
      (response.json \ "nino").as[String]           shouldBe nino.nino
      (response.json \ "name").as[String]           shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean]     shouldBe false
      (response.json \ "utr" \ "status").as[String] shouldBe "noUtr"
      (response.json \ "utr" \ "inactiveEnrolmentUrl")
        .as[String] shouldBe "https://www.gov.uk/register-for-self-assessment"

    }

    "return no utr object if call to CID fails with 500" in {
      accountsFoundMissingSaUtr(nino.nino)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody
      cidWillReturnErrorResponse(500)

      val response = await(getRequestWithAcceptHeader(url))

      response.status                           shouldBe 200
      (response.json \ "nino").as[String]       shouldBe nino.nino
      (response.json \ "name").as[String]       shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean] shouldBe false
      (response.json \ "utr").isEmpty           shouldBe true

    }

    "return no utr object if call to Enrolment Store fails" in {
      accountsFoundMissingSaUtr(nino.nino)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody
      cidPersonForNinoAre(cidPersonJson)
      enrolmentStoreReturnErrorResponse("123123123", 500)

      val response = await(getRequestWithAcceptHeader(url))

      response.status                           shouldBe 200
      (response.json \ "nino").as[String]       shouldBe nino.nino
      (response.json \ "name").as[String]       shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean] shouldBe false
      (response.json \ "utr").isEmpty           shouldBe true

    }

    "return 401 when auth fails" in {
      accountsFound()

      val response = await(wsUrl(url).addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 401
    }

    "return 400 when no journeyId supplied" in {
      accountsFound()

      val response =
        await(wsUrl("/preflight-check").addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 400
    }

    "return 400 when invalid journeyId supplied" in {
      accountsFound()

      val response = await(
        wsUrl("/preflight-check?journeyId=ThisIsAnInvalidJourneyId")
          .addHttpHeaders(acceptJsonHeader)
          .get()
      )
      response.status shouldBe 400
    }
  }
}

class LivePreflightControllerAllEnabledISpec extends LivePreFlightControllerTests {

  "GET /preflight-check" should {

    "return paye link and inactive saUtr link if no active saUtr found" in {
      accountsFound(nino.nino, saUtr.utr, activateUtr = false)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      (response.json \ "annualTaxSummaryLink" \ "link").as[String]        shouldBe "/annual-tax-summary/paye/main"
      (response.json \ "annualTaxSummaryLink" \ "destination").as[String] shouldBe "PAYE"
      (response.json \ "utr" \ "saUtr").as[String]                        shouldBe "123456789"
      (response.json \ "utr" \ "inactiveEnrolmentUrl")
        .as[String] shouldBe "/personal-account/self-assessment"

    }

    "return sa link if active saUtr found" in {
      accountsFound(nino.nino, saUtr.utr)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      (response.json \ "annualTaxSummaryLink" \ "link").as[String]        shouldBe "/annual-tax-summary"
      (response.json \ "annualTaxSummaryLink" \ "destination").as[String] shouldBe "SA"
      (response.json \ "utr" \ "saUtr").as[String]                        shouldBe "123456789"
      (response.json \ "utr" \ "inactiveEnrolmentUrl").isEmpty            shouldBe true
    }
  }
}

class LivePreflightControllerATSLinkDisabledISpec extends LivePreFlightControllerTests {

  override protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(
    config ++
    Map(
      "feature.annualTaxSummaryLink" -> false
    )
  )

  "GET /preflight-check" should {

    "return no ATS link if feature flag is off" in {
      accountsFound(nino.nino, saUtr.utr, activateUtr = false)
      respondToAuditMergedWithNoBody
      respondToAuditWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      (response.json \ "annualTaxSummaryLink").isEmpty shouldBe true
    }
  }
}
