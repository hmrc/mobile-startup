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

package uk.gov.hmrc.mobilestartup.services
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FreeSpecLike, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.JsValue
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ItmpName, Name}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.mobilestartup.TestF
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import eu.timepit.refined.auto._

class PreFlightServiceImplSpec
    extends FreeSpecLike
    with TestF
    with ScalaCheckDrivenPropertyChecks
    with NinoGen
    with Matchers {

  val journeyId: JourneyId = "7f1b5289-5f4d-4150-93a3-ff02dda28375"

  val fullName = ItmpName(givenName = Some("Jennifer"), None, familyName = Some("Thorsteinson"))

  private def dummyConnector: GenericConnector[TestF] =
    new GenericConnector[TestF] {

      override def doGet(
        serviceName: String,
        path:        String,
        hc:          HeaderCarrier
      ): TestF[JsValue] = ???

      override def doPost[T](
        json:         JsValue,
        serviceName:  String,
        path:         String,
        hc:           HeaderCarrier
      )(implicit rds: HttpReads[T]
      ): TestF[T] = ???
    }

  private def service(
    nino:                 Option[Nino],
    saUtr:                Option[SaUtr],
    credentials:          Option[Credentials],
    confidenceLevel:      ConfidenceLevel,
    name:                 Option[ItmpName],
    annualTaxSummaryLink: Option[AnnualTaxSummaryLink],
    connector:            GenericConnector[TestF]
  ): PreFlightService[TestF] = new PreFlightServiceImpl[TestF](connector, 200) {

    override def retrieveAccounts(implicit hc: HeaderCarrier): TestF[
      (Option[Nino], Option[SaUtr], Option[Credentials], ConfidenceLevel, Option[ItmpName], Option[AnnualTaxSummaryLink])
    ] =
      (nino, saUtr, credentials, confidenceLevel, name, annualTaxSummaryLink).pure[TestF]

    override def auditing[T](
      service:     String,
      details:     Map[String, String]
    )(f:           => TestF[T]
    )(implicit hc: HeaderCarrier
    ): TestF[T] = f
  }

  "preFlight" - {
    "should" - {
      "return a response" - {
        "with the expected nino" in forAll { nino: Nino =>
          val sut =
            service(
              Some(nino),
              None,
              Some(Credentials("", "GovernmentGateway")),
              ConfidenceLevel.L200,
              Some(fullName),
              Some(AnnualTaxSummaryLink("/annual-tax-summary", "SA")),
              dummyConnector
            )
          sut.preFlight(journeyId)(HeaderCarrier()).unsafeGet.nino shouldBe Some(nino)
          sut.preFlight(journeyId)(HeaderCarrier()).unsafeGet.annualTaxSummaryLink shouldBe Some(
            AnnualTaxSummaryLink("/annual-tax-summary", "SA")
          )
        }

        "with the expected utr" in forAll { utr: String =>
          val sut =
            service(
              None,
              Some(SaUtr(utr)),
              Some(Credentials("", "GovernmentGateway")),
              ConfidenceLevel.L200,
              Some(fullName),
              Some(AnnualTaxSummaryLink("/annual-tax-summary/paye/main", "PAYE")),
              dummyConnector
            )
          sut.preFlight(journeyId)(HeaderCarrier()).unsafeGet.saUtr shouldBe Some(SaUtr(utr))
          sut.preFlight(journeyId)(HeaderCarrier()).unsafeGet.annualTaxSummaryLink shouldBe Some(
            AnnualTaxSummaryLink("/annual-tax-summary/paye/main", "PAYE")
          )
        }

        {
          implicit val arbConfidenceLevel: Arbitrary[ConfidenceLevel] =
            Arbitrary(Gen.oneOf(ConfidenceLevel.L200, ConfidenceLevel.L300, ConfidenceLevel.L500))

          "routeToIV should be false if the confidence level is 200 or above" in forAll {
            confidenceLevel: ConfidenceLevel =>
              val sut =
                service(None,
                        None,
                        Some(Credentials("", "GovernmentGateway")),
                        confidenceLevel,
                        Some(fullName),
                        None,
                        dummyConnector)
              sut.preFlight(journeyId)(HeaderCarrier()).unsafeGet.routeToIV shouldBe false
          }
        }

        {
          implicit val arbConfidenceLevel: Arbitrary[ConfidenceLevel] =
            Arbitrary(Gen.oneOf(ConfidenceLevel.L50, ConfidenceLevel.L0))

          "routeToIV should be true if the confidence level is below 200" in forAll {
            confidenceLevel: ConfidenceLevel =>
              val sut =
                service(None,
                        None,
                        Some(Credentials("", "GovernmentGateway")),
                        confidenceLevel,
                        Some(fullName),
                        None,
                        dummyConnector)
              sut.preFlight(journeyId)(HeaderCarrier()).unsafeGet.routeToIV shouldBe true
          }
        }

        "and if the auth provided is not 'GovernmentGateway'" - {
          "it should throw an UnsupportedAuthProvider exception" in {
            val sut =
              service(None,
                      None,
                      Some(Credentials("", "NotGovernmentGateway!")),
                      ConfidenceLevel.L200,
                      Some(fullName),
                      None,
                      dummyConnector)
            intercept[UnsupportedAuthProvider](sut.preFlight(journeyId)(HeaderCarrier()).unsafeGet)
          }
        }
      }
    }
  }
}
