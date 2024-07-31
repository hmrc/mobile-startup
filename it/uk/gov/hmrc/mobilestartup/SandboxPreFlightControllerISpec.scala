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

package uk.gov.hmrc.mobilestartup

import play.api.http.HeaderNames
import uk.gov.hmrc.mobilestartup.services.AnnualTaxSummaryLink
import uk.gov.hmrc.mobilestartup.support.BaseISpec
import eu.timepit.refined.auto._
import uk.gov.hmrc.domain.Nino

class SandboxPreFlightControllerISpec extends BaseISpec {

  private val headerThatSucceeds =
    Seq(HeaderNames.ACCEPT -> "application/vnd.hmrc.1.0+json", "X-MOBILE-USER-ID" -> "208606423740")

  private def withSandboxControl(value: String) = Seq("SANDBOX-CONTROL" -> value)

  def withJourneyParam(journeyId: String) = s"journeyId=$journeyId"

  override val nino = Nino("CS700100A")

  //Temporarily removed to find out prod demo user's credId

//  "POST of /preflight-check with X-MOBILE-USER-ID header" should {
//
//    "successfully switch to the sandbox preflight" in {
//      val response =
//        await(wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}").addHttpHeaders(headerThatSucceeds: _*).get())
//
//      response.status                                                   shouldBe 200
//      (response.json \ "nino").as[String]                               shouldBe nino.nino
//      (response.json \ "routeToIV").as[Boolean]                         shouldBe false
//      (response.json \ "routeToTEN").as[Boolean]                        shouldBe false
//      (response.json \ "annualTaxSummaryLink").as[AnnualTaxSummaryLink] shouldBe AnnualTaxSummaryLink("/", "PAYE")
//    }
//
//    "return routeToIV = true when SANDBOX-CONTROL header = ROUTE-TO-IV" in {
//      val response = await(
//        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
//          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ROUTE-TO-IV"): _*)
//          .get()
//      )
//
//      response.status                           shouldBe 200
//      (response.json \ "nino").as[String]       shouldBe nino.nino
//      (response.json \ "routeToIV").as[Boolean] shouldBe true
//    }
//
//    "return routeToTEN = true when SANDBOX-CONTROL header = ROUTE-TO-TEN" in {
//      val response = await(
//        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
//          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ROUTE-TO-TEN"): _*)
//          .get()
//      )
//
//      response.status                            shouldBe 200
//      (response.json \ "nino").as[String]        shouldBe nino.nino
//      (response.json \ "routeToIV").as[Boolean]  shouldBe false
//      (response.json \ "routeToTEN").as[Boolean] shouldBe true
//    }
//
//    "return unauthorized when SANDBOX-CONTROL header = ERROR-401" in {
//      val response = await(
//        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
//          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-401"): _*)
//          .get()
//      )
//
//      response.status shouldBe 401
//    }
//
//    "return unauthorized when SANDBOX-CONTROL header = ERROR-403" in {
//      val response = await(
//        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
//          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-403"): _*)
//          .get()
//      )
//
//      response.status shouldBe 403
//    }
//
//    "return unauthorized when SANDBOX-CONTROL header = ERROR-500" in {
//      val response = await(
//        wsUrl(s"/preflight-check?${withJourneyParam(journeyId)}")
//          .addHttpHeaders(headerThatSucceeds ++ withSandboxControl("ERROR-500"): _*)
//          .get()
//      )
//
//      response.status shouldBe 500
//    }
//
//    "return 400 if journeyId not supplied" in {
//      val response = await(wsUrl("/preflight-check").addHttpHeaders(headerThatSucceeds: _*).get())
//
//      response.status shouldBe 400
//    }
//
//    "return 400 if journeyId is invalid" in {
//      val response = await(
//        wsUrl(s"/preflight-check?${withJourneyParam("ThisIsAnInvalidJourneyId")}")
//          .addHttpHeaders(headerThatSucceeds: _*)
//          .get()
//      )
//      response.status shouldBe 400
//    }
//  }
}
