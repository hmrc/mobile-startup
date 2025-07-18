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

package uk.gov.hmrc.mobilestartup.services
import cats.implicits.*
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.{BaseSpec, StartupTestData}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.types.JourneyId
import eu.timepit.refined.auto.*
import uk.gov.hmrc.mobilestartup.model.Activated
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.fromStringtoLinkDestination

import scala.concurrent.ExecutionContext

class PreFlightServiceImplSpec extends BaseSpec with StartupTestData {

  override val journeyId: JourneyId        = JourneyId.from("7f1b5289-5f4d-4150-93a3-ff02dda28375").toOption.get
  implicit val ec:        ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val nino:               Nino             = Nino("CS700100A")
  val utr:                SaUtr            = SaUtr("123123123")

  private def service(
    nino:                 Option[Nino],
    saUtr:                Option[SaUtr],
    credentials:          Option[Credentials],
    confidenceLevel:      ConfidenceLevel,
    annualTaxSummaryLink: Option[AnnualTaxSummaryLink],
    enrolments:           Enrolments,
    connector:            GenericConnector[TestF],
    credId:               Option[String]
  ): PreFlightService[TestF] = new PreFlightServiceImpl[TestF](connector, 200, "appStoreId", "appDemoId") {

    override def retrieveAccounts(implicit hc: HeaderCarrier): TestF[
      (Option[Nino],
       Option[SaUtr],
       Option[Credentials],
       ConfidenceLevel,
       Option[AnnualTaxSummaryLink],
       Enrolments,
       Option[String])
    ] =
      (nino, saUtr, credentials, confidenceLevel, annualTaxSummaryLink, enrolments, credId).pure[TestF]

    override def auditing[T](
      service:     String,
      details:     Map[String, String]
    )(f:           => TestF[T]
    )(implicit hc: HeaderCarrier
    ): TestF[T] = f

    def doesUserHaveMultipleGGIDs(
      enrolments:  Enrolments,
      nino:        Option[Nino]
    )(implicit hc: HeaderCarrier
    ): Boolean = false

    def getUtr(
      foundUtr:    Option[SaUtr],
      foundNino:   Option[Nino],
      enrolments:  Enrolments
    )(implicit hc: HeaderCarrier
    ): TestF[Option[Utr]] = Some(Utr(foundUtr, Activated)).pure[TestF]
  }

  "preFlight" should {
    "return a response with the expected nino" in {
      val sut =
        service(
          Some(nino),
          None,
          Some(Credentials("", "GovernmentGateway")),
          ConfidenceLevel.L200,
          Some(AnnualTaxSummaryLink("/annual-tax-summary", fromStringtoLinkDestination("SA"))),
          Enrolments(Set.empty),
          dummyConnector(),
          Some("11223344")
        )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.nino shouldBe Some(nino)
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.annualTaxSummaryLink shouldBe Some(
        AnnualTaxSummaryLink("/annual-tax-summary", fromStringtoLinkDestination("SA"))
      )
    }

    "return a response with the expected utr" in {
      val sut =
        service(
          None,
          Some(utr),
          Some(Credentials("", "GovernmentGateway")),
          ConfidenceLevel.L200,
          Some(AnnualTaxSummaryLink("/annual-tax-summary/paye/main", fromStringtoLinkDestination("PAYE"))),
          Enrolments(Set.empty),
          dummyConnector(),
          Some("11223344")
        )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.utr.get.saUtr shouldBe Some(utr)
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.annualTaxSummaryLink shouldBe Some(
        AnnualTaxSummaryLink("/annual-tax-summary/paye/main", fromStringtoLinkDestination("PAYE"))
      )
    }

    "return a response with demoAccount = false if the internalId does not match a demo account Id" in {
      val sut =
        service(
          None,
          Some(utr),
          Some(Credentials("", "GovernmentGateway")),
          ConfidenceLevel.L200,
          Some(AnnualTaxSummaryLink("/annual-tax-summary/paye/main", fromStringtoLinkDestination("PAYE"))),
          Enrolments(Set.empty),
          dummyConnector(),
          Some("11223344")
        )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.demoAccount shouldBe false
    }

    "routeToIV should be false if the confidence level is 200 or above" in {
      val sut =
        service(None,
                None,
                Some(Credentials("", "GovernmentGateway")),
                ConfidenceLevel.L250,
                None,
                Enrolments(Set.empty),
                dummyConnector(),
                Some("11223344"))
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
                dummyConnector(),
                Some("11223344"))
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.routeToIV shouldBe true
    }

    "return the sandbox data if the appStoreAccount internal ID is returned" in {
      val sut =
        service(
          Some(nino),
          None,
          Some(Credentials("", "GovernmentGateway")),
          ConfidenceLevel.L200,
          Some(AnnualTaxSummaryLink("/annual-tax-summary", fromStringtoLinkDestination("SA"))),
          Enrolments(Set.empty),
          dummyConnector(),
          Some("appStoreId")
        )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.nino shouldBe Some(nino)
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.annualTaxSummaryLink shouldBe Some(
        AnnualTaxSummaryLink("/", fromStringtoLinkDestination("PAYE"))
      )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.demoAccount shouldBe true
    }

    "return the sandbox data if the appDemoAccount internal ID is returned" in {
      val sut =
        service(
          Some(nino),
          None,
          Some(Credentials("", "GovernmentGateway")),
          ConfidenceLevel.L200,
          Some(AnnualTaxSummaryLink("/annual-tax-summary", fromStringtoLinkDestination("SA"))),
          Enrolments(Set.empty),
          dummyConnector(),
          Some("appDemoId")
        )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.nino shouldBe Some(nino)
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.annualTaxSummaryLink shouldBe Some(
        AnnualTaxSummaryLink("/", fromStringtoLinkDestination("PAYE"))
      )
      sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet.demoAccount shouldBe true
    }

    "if the auth provided is not 'GovernmentGateway' it should throw an UnsupportedAuthProvider exception" in {
      val sut =
        service(None,
                None,
                Some(Credentials("", "NotGovernmentGateway!")),
                ConfidenceLevel.L200,
                None,
                Enrolments(Set.empty),
                dummyConnector(),
                Some("11223344"))
      intercept[UnsupportedAuthProvider](sut.preFlight(journeyId)(HeaderCarrier(), ec).unsafeGet)
    }
  }
}
