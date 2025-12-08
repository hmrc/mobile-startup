/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.domain.{Nino, SaUtr, TaxIds}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.{Activated, CidPerson, EnrolmentStoreResponse, NoEnrolment, NotYetActivated, WrongAccount}
import uk.gov.hmrc.mobilestartup.{BaseSpec, StartupTestData}
import uk.gov.hmrc.mobilestartup.model.types.JourneyId
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class LivePreFlightServiceSpec extends BaseSpec with StartupTestData {

  override val journeyId:   JourneyId                = JourneyId.from("7f1b5289-5f4d-4150-93a3-ff02dda28375").toOption.get
  implicit val ec:          ExecutionContext         = scala.concurrent.ExecutionContext.Implicits.global
  val nino:                 Nino                     = Nino("CS700100A")
  val saUtr:                SaUtr                    = SaUtr("123123123")
  val mockGenericConnector: GenericConnector[Future] = mock[GenericConnector[Future]]
  val mockAuditConnector:   AuditConnector           = mock[AuditConnector]
  val mockAuthConnnector = mock[AuthConnector]
  val mockAuditservice   = mock[AuditService]

  def service(
    confLevel:            Int,
    annualTaxSummaryLink: Boolean = false
  ) = new LivePreFlightService(
    genericConnector                = mockGenericConnector,
    auditConnector                  = mockAuditConnector,
    authConnector                   = mockAuthConnnector,
    appName                         = "mobile-startup",
    confLevel                       = confLevel,
    showATSLink                     = annualTaxSummaryLink,
    multipleGGIDCheckEnabledIos     = false,
    multipleGGIDCheckEnabledAndroid = false,
    storeReviewAccountInternalId    = "INT1234",
    appTeamAccountInternalId        = "ATMInt1234",
    auditService                    = mockAuditservice
  )

  def mockGetUtrFromCID(response: Future[CidPerson]) =
    (mockGenericConnector
      .cidGet(_: String, _: String, _: HeaderCarrier))
      .expects(*, *, *)
      .returning(response)

  def mockDoesUtrHavePrincipalIds(response: Future[EnrolmentStoreResponse]) =
    (mockGenericConnector
      .enrolmentStoreGet(_: String, _: String, _: HeaderCarrier))
      .expects(*, *, *)
      .returning(response)

  "getUTR" should {
    "return some utr if only IR-SA enroll,ment is there" in {
      val livePreFlightService = service(250)
      val result: Future[Option[Utr]] = livePreFlightService.getUtr(
        Some(saUtr),
        Some(nino),
        Enrolments(
          Set(
            Enrolment(key = "IR-SA", identifiers = Seq(EnrolmentIdentifier("UTR", saUtr.value)), state = "Activated")
          )
        )
      )
      await(result) shouldBe (Some(Utr(Some(saUtr), Activated, None)))
    }

    "return some utr if both  IR-SA and MTD enrollment are there" in {
      val livePreFlightService = service(250)
      val result: Future[Option[Utr]] = livePreFlightService.getUtr(
        Some(saUtr),
        Some(nino),
        Enrolments(
          Set(
            Enrolment(key         = "IR-SA", identifiers = Seq(EnrolmentIdentifier("UTR", saUtr.value)), state = "Activated"),
            Enrolment(key         = "HMRC-MTD-ID",
                      identifiers = Seq(EnrolmentIdentifier("MTDITID", saUtr.value)),
                      state       = "Activated")
          )
        )
      )
      await(result) shouldBe (Some(Utr(Some(saUtr), Activated, None)))
    }

    "return some utr if only MTD enrollment is  there and utyr is also there" in {
      val livePreFlightService = service(250)
      val result: Future[Option[Utr]] = livePreFlightService.getUtr(
        Some(saUtr),
        Some(nino),
        Enrolments(
          Set(
            Enrolment(key         = "HMRC-MTD-ID",
                      identifiers = Seq(EnrolmentIdentifier("MTDITID", saUtr.value)),
                      state       = "Activated")
          )
        )
      )
      await(result) shouldBe (Some(Utr(Some(saUtr), Activated, None)))
    }

    "return some utr with NOt activated status if no enrolments are present but utr is there" in {
      val livePreFlightService = service(250)
      val result: Future[Option[Utr]] = livePreFlightService.getUtr(
        Some(saUtr),
        Some(nino),
        Enrolments(
          Set.empty
        )
      )
      await(result) shouldBe (Some(Utr(Some(saUtr), NotYetActivated, NotYetActivated.link)))
    }

    "return Not Found UTR when No enrolment present and no utr present and no utr from citizen details" in {
      val livePreFlightService = service(250)

      val cidPerson = CidPerson(
        TaxIds(
          Set(
            nino
          )
        )
      )
      mockGetUtrFromCID(Future.successful(cidPerson))
      val result: Future[Option[Utr]] = livePreFlightService.getUtr(
        None,
        Some(nino),
        Enrolments(
          Set.empty
        )
      )
      await(result) shouldBe Some(Utr.noUtr)
    }

    "return None when No enrolment present and no utr present and some utr from citizen details and failed response from enrolment-store-proxy" in {
      val livePreFlightService = service(250)

      val cidPerson = CidPerson(
        TaxIds(
          Set(
            nino,
            saUtr
          )
        )
      )

      val enrolmentStoreResponse = EnrolmentStoreResponse(
        Seq.empty
      )
      mockGetUtrFromCID(Future.successful(cidPerson))
      mockDoesUtrHavePrincipalIds(Future.failed(UpstreamErrorResponse("error", 500)))
      val result: Future[Option[Utr]] = livePreFlightService.getUtr(
        None,
        Some(nino),
        Enrolments(
          Set.empty
        )
      )
      await(result) shouldBe None
    }

    "return some Utr if no enrolment present , no utr, citizen details give some utr and false from does utrHavePrincipalIds " in {
      val livePreFlightService = service(250)

      val cidPerson = CidPerson(
        TaxIds(
          Set(
            nino,
            saUtr
          )
        )
      )

      val enrolmentStoreResponse = EnrolmentStoreResponse(
        Seq.empty
      )
      mockGetUtrFromCID(Future.successful(cidPerson))
      mockDoesUtrHavePrincipalIds(Future.successful(enrolmentStoreResponse))
      val result: Future[Option[Utr]] = livePreFlightService.getUtr(
        None,
        Some(nino),
        Enrolments(
          Set.empty
        )
      )
      await(result) shouldBe Some(Utr(Some(saUtr), NoEnrolment, NoEnrolment.link))
    }

    "return some Utr if no enrolment present , no utr, citizen details give some utr and true from does utrHavePrincipalIds " in {
      val livePreFlightService = service(250)

      val cidPerson = CidPerson(
        TaxIds(
          Set(
            nino,
            saUtr
          )
        )
      )

      val enrolmentStoreResponse = EnrolmentStoreResponse(
        Seq("id1")
      )
      mockGetUtrFromCID(Future.successful(cidPerson))
      mockDoesUtrHavePrincipalIds(Future.successful(enrolmentStoreResponse))
      val result: Future[Option[Utr]] = livePreFlightService.getUtr(
        None,
        Some(nino),
        Enrolments(
          Set.empty
        )
      )
      await(result) shouldBe Some(Utr(Some(saUtr), WrongAccount, WrongAccount.link))
    }

  }
}
