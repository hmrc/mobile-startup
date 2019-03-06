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
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsString, JsValue}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.BaseSpec
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class LiveStartupServiceTest extends BaseSpec {

  private val helpToSave         = "helpToSave"
  private val taxCreditsRenewals = "taxCreditRenewals"
  private val taxSummary         = "taxSummary"
  val successfulResponse         = JsString("success")

  private val htsSuccessResponse: JsValue = successfulResponse
  private val tcrSuccessResponse: JsValue = obj("submissionsState" -> "open")
  private val tsSuccessResponse:  JsValue = successfulResponse

  private def dummyConnector(
    htsResponse:        Future[JsValue] = Future.successful(htsSuccessResponse),
    tcrResponse:        Future[JsValue] = Future.successful(tcrSuccessResponse),
    taxSummaryResponse: Future[JsValue] = Future.successful(tsSuccessResponse)
  ): GenericConnector =
    new GenericConnector {
      override def doGet(serviceName: String, path: String, hc: HeaderCarrier)(implicit ec: ExecutionContext): Future[JsValue] =
        serviceName match {
          case "mobile-help-to-save"        => htsResponse
          case "mobile-tax-credits-renewal" => tcrResponse
          case "mobile-paye"                => taxSummaryResponse
          case _                            => Future.successful(obj())
        }
    }

  "a fully successful response" should {
    "contain success entries for each service" in {
      val sut = new LiveStartupService(dummyConnector())

      val result: JsObject = await(sut.startup("nino", None)(HeaderCarrier()))

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ taxSummary).toOption.value         shouldBe tsSuccessResponse
    }
  }

  "a response" should {
    "contain an empty-object entry for help-to-save" in {
      val sut = new LiveStartupService(dummyConnector(htsResponse = Future.failed(new Exception("hts failed"))))

      val result: JsObject = await(sut.startup("nino", None)(HeaderCarrier()))

      (result \ helpToSave).toOption.value         shouldBe obj()
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ taxSummary).toOption.value         shouldBe tsSuccessResponse
    }

    "contain an error entry for tcr" in {
      val sut = new LiveStartupService(dummyConnector(tcrResponse = Future.failed(new Exception("tcr failed"))))

      val result: JsObject = await(sut.startup("nino", None)(HeaderCarrier()))

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe obj("submissionsState" -> "error")
      (result \ taxSummary).toOption.value         shouldBe tsSuccessResponse
    }

    "contain an empty-object entry for tax summary" in {
      val sut = new LiveStartupService(dummyConnector(taxSummaryResponse = Future.failed(new Exception("tax summary failed"))))

      val result: JsObject = await(sut.startup("nino", None)(HeaderCarrier()))

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ taxSummary).toOption.value         shouldBe obj()
    }
  }
}
