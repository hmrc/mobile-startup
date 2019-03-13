package uk.gov.hmrc.mobilestartup.services
import java.util.UUID

import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpecLike, Matchers}
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{ConfidenceLevel, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.mobilestartup.TestF
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.controllers.DeviceVersion

import scala.util.{Success, Try}

class PreflightServiceImplTest extends FreeSpecLike with Matchers with TestF with GeneratorDrivenPropertyChecks with NinoGen {

  private def dummyConnector(versionCheckResult: Boolean): GenericConnector[TestF] =
    new GenericConnector[TestF] {
      override def doGet(serviceName: String, path: String, hc: HeaderCarrier): TestF[JsValue] = ???

      override def doPost[T](json: JsValue, serviceName: String, path: String, hc: HeaderCarrier)(implicit rds: HttpReads[T]): TestF[T] =
        obj("upgradeRequired" -> versionCheckResult).asInstanceOf[T].pure[TestF]
    }

  private def service(
    nino:            Option[Nino],
    saUtr:           Option[SaUtr],
    credentials:     Credentials,
    confidenceLevel: ConfidenceLevel,
    connector:       GenericConnector[TestF]): PreflightService[TestF] = new PreflightServiceImpl[TestF](connector, 200) {
    override def retrieveAccounts(implicit hc: HeaderCarrier): TestF[(Option[Nino], Option[SaUtr], Credentials, ConfidenceLevel)] =
      (nino, saUtr, credentials, confidenceLevel).pure[TestF]

    override def auditing[T](service: String, details: Map[String, String])(f: => TestF[T])(implicit hc: HeaderCarrier): TestF[T] = f
  }

  "preFlight" - {
    "should" - {
      "return a response" - {
        "with the expected versionCheckResult" in forAll { b: Boolean =>
          val sut = service(None, None, Credentials("", "GovernmentGateway"), ConfidenceLevel.L200, dummyConnector(b))
          sut.preFlight(DeviceVersion("", ""), None)(HeaderCarrier()).unsafeGet.upgradeRequired shouldBe b
        }

        "and the expected nino" in forAll { nino: Nino =>
          val sut = service(Some(nino), None, Credentials("", "GovernmentGateway"), ConfidenceLevel.L200, dummyConnector(false))
          sut.preFlight(DeviceVersion("", ""), None)(HeaderCarrier()).unsafeGet.accounts.nino shouldBe Some(nino)
        }

        "and the expected utr" in forAll { utr: String =>
          val sut = service(None, Some(SaUtr(utr)), Credentials("", "GovernmentGateway"), ConfidenceLevel.L200, dummyConnector(false))
          sut.preFlight(DeviceVersion("", ""), None)(HeaderCarrier()).unsafeGet.accounts.saUtr shouldBe Some(SaUtr(utr))
        }

        {
          implicit val arbConfidenceLevel: Arbitrary[ConfidenceLevel] =
            Arbitrary(Gen.oneOf(ConfidenceLevel.L200, ConfidenceLevel.L300, ConfidenceLevel.L500))

          "and routeToIV should be false if the confidence level is 200 or above" in forAll { confidenceLevel: ConfidenceLevel =>
            val sut = service(None, None, Credentials("", "GovernmentGateway"), confidenceLevel, dummyConnector(false))
            sut.preFlight(DeviceVersion("", ""), None)(HeaderCarrier()).unsafeGet.accounts.routeToIV shouldBe false
          }
        }

        {
          implicit val arbConfidenceLevel: Arbitrary[ConfidenceLevel] =
            Arbitrary(Gen.oneOf(ConfidenceLevel.L50, ConfidenceLevel.L0))

          "and routeToIV should be true if the confidence level is below 200" in forAll { confidenceLevel: ConfidenceLevel =>
            val sut = service(None, None, Credentials("", "GovernmentGateway"), confidenceLevel, dummyConnector(false))
            sut.preFlight(DeviceVersion("", ""), None)(HeaderCarrier()).unsafeGet.accounts.routeToIV shouldBe true
          }
        }

        "and either" - {
          "generate a valid uuid for the journeyId if none is provided" in {
            val sut       = service(None, None, Credentials("", "GovernmentGateway"), ConfidenceLevel.L200, dummyConnector(false))
            val journeyId = sut.preFlight(DeviceVersion("", ""), None)(HeaderCarrier()).unsafeGet.accounts.journeyId
            Try(UUID.fromString(journeyId)) shouldBe a[Success[_]]
          }

          "or return the provided journeyId" in forAll { uuid: UUID =>
            val sut = service(None, None, Credentials("", "GovernmentGateway"), ConfidenceLevel.L200, dummyConnector(false))
            sut.preFlight(DeviceVersion("", ""), Some(uuid.toString))(HeaderCarrier()).unsafeGet.accounts.journeyId shouldBe uuid.toString
          }
        }

        "and if the auth provided is not 'GovernmentGateway'" - {
          "it should throw an UnsupportedAuthProvider exception" in {
            val sut = service(None, None, Credentials("", "NotGovernmentGateway!"), ConfidenceLevel.L200, dummyConnector(false))
            intercept[UnsupportedAuthProvider](sut.preFlight(DeviceVersion("", ""), None)(HeaderCarrier()).unsafeGet)
          }
        }
      }
    }
  }
}
