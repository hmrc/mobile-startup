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
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpecLike, Matchers}
import play.api.libs.json.JsValue
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{ConfidenceLevel, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.mobilestartup.TestF
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector

class PreFlightServiceImplSpec extends FreeSpecLike with Matchers with TestF with GeneratorDrivenPropertyChecks with NinoGen {

  private def dummyConnector: GenericConnector[TestF] =
    new GenericConnector[TestF] {
      override def doGet(serviceName: String, path: String, hc: HeaderCarrier): TestF[JsValue] = ???

      override def doPost[T](json: JsValue, serviceName: String, path: String, hc: HeaderCarrier)(implicit rds: HttpReads[T]): TestF[T] = ???
    }

  private def service(
    nino:            Option[Nino],
    saUtr:           Option[SaUtr],
    credentials:     Credentials,
    confidenceLevel: ConfidenceLevel,
    connector:       GenericConnector[TestF]): PreFlightService[TestF] = new PreFlightServiceImpl[TestF](connector, 200) {
    override def retrieveAccounts(implicit hc: HeaderCarrier): TestF[(Option[Nino], Option[SaUtr], Credentials, ConfidenceLevel)] =
      (nino, saUtr, credentials, confidenceLevel).pure[TestF]

    override def auditing[T](service: String, details: Map[String, String])(f: => TestF[T])(implicit hc: HeaderCarrier): TestF[T] = f
  }

  "preFlight" - {
    "should" - {
      "return a response" - {
        "with the expected nino" in forAll { nino: Nino =>
          val sut = service(Some(nino), None, Credentials("", "GovernmentGateway"), ConfidenceLevel.L200, dummyConnector)
          sut.preFlight(None)(HeaderCarrier()).unsafeGet.nino shouldBe Some(nino)
        }

        "with the expected utr" in forAll { utr: String =>
          val sut = service(None, Some(SaUtr(utr)), Credentials("", "GovernmentGateway"), ConfidenceLevel.L200, dummyConnector)
          sut.preFlight(None)(HeaderCarrier()).unsafeGet.saUtr shouldBe Some(SaUtr(utr))
        }

        {
          implicit val arbConfidenceLevel: Arbitrary[ConfidenceLevel] =
            Arbitrary(Gen.oneOf(ConfidenceLevel.L200, ConfidenceLevel.L300, ConfidenceLevel.L500))

          "routeToIV should be false if the confidence level is 200 or above" in forAll { confidenceLevel: ConfidenceLevel =>
            val sut = service(None, None, Credentials("", "GovernmentGateway"), confidenceLevel, dummyConnector)
            sut.preFlight(None)(HeaderCarrier()).unsafeGet.routeToIV shouldBe false
          }
        }

        {
          implicit val arbConfidenceLevel: Arbitrary[ConfidenceLevel] =
            Arbitrary(Gen.oneOf(ConfidenceLevel.L50, ConfidenceLevel.L0))

          "routeToIV should be true if the confidence level is below 200" in forAll { confidenceLevel: ConfidenceLevel =>
            val sut = service(None, None, Credentials("", "GovernmentGateway"), confidenceLevel, dummyConnector)
            sut.preFlight(None)(HeaderCarrier()).unsafeGet.routeToIV shouldBe true
          }
        }

        "and if the auth provided is not 'GovernmentGateway'" - {
          "it should throw an UnsupportedAuthProvider exception" in {
            val sut = service(None, None, Credentials("", "NotGovernmentGateway!"), ConfidenceLevel.L200, dummyConnector)
            intercept[UnsupportedAuthProvider](sut.preFlight(None)(HeaderCarrier()).unsafeGet)
          }
        }
      }
    }
  }
}
