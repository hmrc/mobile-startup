/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json.JsValue
import uk.gov.hmrc.auth.core.retrieve.{Credentials}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.mobilestartup.{BaseSpec, TestF}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import eu.timepit.refined.auto._
import uk.gov.hmrc.mobilestartup.model.{CidPerson, EnrolmentStoreResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext

class PreFlightServiceImplSpec extends BaseSpec with TestF {

  override val journeyId: JourneyId        = "7f1b5289-5f4d-4150-93a3-ff02dda28375"
  implicit val ec:        ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val nino:               Nino             = Nino("CS700100A")
  val utr:                SaUtr            = SaUtr("123123123")

  private def dummyConnector: GenericConnector[TestF] =
    new GenericConnector[TestF] {

      override def doGet(
        serviceName: String,
        path:        String,
        hc:          HeaderCarrier
      ): TestF[JsValue] = ???

      override def cidGet(
        serviceName: String,
        path:        String,
        hc:          HeaderCarrier
      ): TestF[CidPerson] = ???

      override def enrolmentStoreGet(
        serviceName: String,
        path:        String,
        hc:          HeaderCarrier
      ): TestF[EnrolmentStoreResponse] = ???

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
    annualTaxSummaryLink: Option[AnnualTaxSummaryLink],
    enrolments:           Enrolments,
    connector:            GenericConnector[TestF]
  ): PreFlightService[TestF] = new PreFlightServiceImpl[TestF](connector, 200) {

    override def retrieveAccounts(implicit hc: HeaderCarrier): TestF[
      (Option[Nino], Option[SaUtr], Option[Credentials], ConfidenceLevel, Option[AnnualTaxSummaryLink], Enrolments)
    ] =
      (nino, saUtr, credentials, confidenceLevel, annualTaxSummaryLink, enrolments).pure[TestF]

    override def auditing[T](
      service:     String,
      details:     Map[String, String]
    )(f:           => TestF[T]
    )(implicit hc: HeaderCarrier
    ): TestF[T] = f

    def doesUserHaveMultipleGGIDs(enrolments: Enrolments)(implicit hc: HeaderCarrier): Boolean = false

    def getUtr(
      foundUtr:    Option[SaUtr],
      foundNino:   Option[Nino],
      enrolments:  Enrolments
    )(implicit hc: HeaderCarrier
    ): TestF[Option[Utr]] = None.pure[TestF]
  }

  "preFlight" should {
    "return a response with the expected nino" in {
      val sut =
        service(
          Some(nino),
          None,
          Some(Credentials("", "GovernmentGateway")),
          ConfidenceLevel.L200,
          Some(AnnualTaxSummaryLink("/annual-tax-summary", "SA")),
          Enrolments(Set.empty),
          dummyConnector
        )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.nino shouldBe Some(nino)
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.annualTaxSummaryLink shouldBe Some(
        AnnualTaxSummaryLink("/annual-tax-summary", "SA")
      )
    }

    "return a response with the expected utr" in {
      val sut =
        service(
          None,
          Some(utr),
          Some(Credentials("", "GovernmentGateway")),
          ConfidenceLevel.L200,
          Some(AnnualTaxSummaryLink("/annual-tax-summary/paye/main", "PAYE")),
          Enrolments(Set.empty),
          dummyConnector
        )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.saUtr shouldBe Some(utr)
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.annualTaxSummaryLink shouldBe Some(
        AnnualTaxSummaryLink("/annual-tax-summary/paye/main", "PAYE")
      )
    }

    "routeToIV should be false if the confidence level is 200 or above" in {
      val sut =
        service(None,
                None,
                Some(Credentials("", "GovernmentGateway")),
                ConfidenceLevel.L250,
                None,
                Enrolments(Set.empty),
                dummyConnector)
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.routeToIV shouldBe false
    }

    "routeToIV should be true if the confidence level is below 200" in {
      val sut =
        service(None,
                None,
                Some(Credentials("", "GovernmentGateway")),
                ConfidenceLevel.L50,
                None,
                Enrolments(Set.empty),
                dummyConnector)
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.routeToIV shouldBe true
    }

    "if the auth provided is not 'GovernmentGateway' it should throw an UnsupportedAuthProvider exception" in {
      val sut =
        service(None,
                None,
                Some(Credentials("", "NotGovernmentGateway!")),
                ConfidenceLevel.L200,
                None,
                Enrolments(Set.empty),
                dummyConnector)
      intercept[UnsupportedAuthProvider](sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet)
    }
  }
}
