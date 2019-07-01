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
import cats.implicits._
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsString, JsValue}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.{BaseSpec, TestF}

class StartupServiceImplTest extends BaseSpec with TestF {

  private val helpToSave         = "helpToSave"
  private val taxCreditsRenewals = "taxCreditRenewals"
  private val taxSummary         = "taxSummary"
  val successfulResponse         = JsString("success")

  private val htsSuccessResponse: JsValue = successfulResponse
  private val tcrSuccessResponse: JsValue = obj("submissionsState" -> "open")
  private val tsSuccessResponse:  JsValue = successfulResponse

  private def dummyConnector(
    htsResponse:        TestF[JsValue] = htsSuccessResponse.pure[TestF],
    tcrResponse:        TestF[JsValue] = tcrSuccessResponse.pure[TestF],
    taxSummaryResponse: TestF[JsValue] = tsSuccessResponse.pure[TestF]
  ): GenericConnector[TestF] =
    new GenericConnector[TestF] {
      override def doGet(serviceName: String, path: String, hc: HeaderCarrier): TestF[JsValue] =
        serviceName match {
          case "mobile-help-to-save"        => htsResponse
          case "mobile-tax-credits-renewal" => tcrResponse
          case "mobile-paye"                => taxSummaryResponse
          case _                            => obj().pure[TestF]
        }

      override def doPost[T](json: JsValue, serviceName: String, path: String, hc: HeaderCarrier)(implicit rds: HttpReads[T]): TestF[T] = ???
    }

  "a fully successful response" should {
    "contain success entries for each service" in {
      val sut = new StartupServiceImpl[TestF](dummyConnector(), false)

      val result: JsObject = sut.startup("nino", None)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ taxSummary).toOption.value         shouldBe tsSuccessResponse
    }
  }

  "a response" should {
    "not contain a taxSummary section if the userPanelSignUp flag is set to true" in {
      val sut = new StartupServiceImpl[TestF](dummyConnector(), true)

      val result: JsObject = sut.startup("nino", None)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ taxSummary).toOption               shouldBe None
    }

    "contain an empty-object entry for help-to-save when the hts call fails" in {
      val sut = new StartupServiceImpl[TestF](dummyConnector(htsResponse = new Exception("hts failed").error), false)

      val result: JsObject = sut.startup("nino", None)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe obj()
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ taxSummary).toOption.value         shouldBe tsSuccessResponse
    }

    "contain an error entry for tcr when the tcr call fails" in {
      val sut = new StartupServiceImpl[TestF](dummyConnector(tcrResponse = new Exception("tcr failed").error), false)

      val result: JsObject = sut.startup("nino", None)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe obj("submissionsState" -> "error")
      (result \ taxSummary).toOption.value         shouldBe tsSuccessResponse
    }

    "contain an empty-object entry for tax summary when the tax summary call fails" in {
      val sut =
        new StartupServiceImpl[TestF](dummyConnector(taxSummaryResponse = new Exception("tax summary failed").error), false)

      val result: JsObject = sut.startup("nino", None)(HeaderCarrier()).unsafeGet

      (result \ helpToSave).toOption.value         shouldBe htsSuccessResponse
      (result \ taxCreditsRenewals).toOption.value shouldBe tcrSuccessResponse
      (result \ taxSummary).toOption.value         shouldBe obj()
    }
  }
}
