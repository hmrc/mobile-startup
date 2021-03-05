package uk.gov.hmrc.mobilestartup

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilestartup.support.BaseISpec
import uk.gov.hmrc.mobilestartup.stubs.AuthStub._
import uk.gov.hmrc.mobilestartup.stubs.AuditStub._
import eu.timepit.refined.auto._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Future

trait LivePreFlightControllerTests extends BaseISpec {
  val nino:      Nino      = Nino("AA000006C")
  val saUtr:     SaUtr     = SaUtr("123456789")
  val journeyId: JourneyId = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val url:       String    = s"/preflight-check?journeyId=$journeyId"

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

      val response = await(getRequestWithAcceptHeader(url))

      response.status                           shouldBe 200
      (response.json \ "nino").as[String]       shouldBe nino.nino
      (response.json \ "saUtr").as[String]      shouldBe saUtr.utr
      (response.json \ "name").as[String]       shouldBe "Test User"
      (response.json \ "routeToIV").as[Boolean] shouldBe false

    }

    "return account details with name if itmpName not available" in {
      accountsFoundMissingItmpName(nino.nino, saUtr.utr)
      respondToAuditMergedWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      response.status                           shouldBe 200
      (response.json \ "nino").as[String]       shouldBe nino.nino
      (response.json \ "saUtr").as[String]      shouldBe saUtr.utr
      (response.json \ "name").as[String]       shouldBe "TestUser2"
      (response.json \ "routeToIV").as[Boolean] shouldBe false

    }

    "return account details with no name if itmpName and name not available" in {
      accountsFoundMissingItmpName(nino.nino, saUtr.utr, bothNamesMissing = true)
      respondToAuditMergedWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      response.status                           shouldBe 200
      (response.json \ "nino").as[String]       shouldBe nino.nino
      (response.json \ "saUtr").as[String]      shouldBe saUtr.utr
      (response.json \ "name").isEmpty          shouldBe true
      (response.json \ "routeToIV").as[Boolean] shouldBe false

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

    "return paye link if no active saUtr found" in {
      accountsFound(nino.nino, saUtr.utr, activateUtr = false)
      respondToAuditMergedWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      (response.json \ "annualTaxSummaryLink" \ "link").as[String]        shouldBe "/annual-tax-summary/paye/main"
      (response.json \ "annualTaxSummaryLink" \ "destination").as[String] shouldBe "PAYE"
    }

    "return sa link if active saUtr found" in {
      accountsFound(nino.nino, saUtr.utr)
      respondToAuditMergedWithNoBody

      val response = await(getRequestWithAcceptHeader(url))

      (response.json \ "annualTaxSummaryLink" \ "link").as[String]        shouldBe "/annual-tax-summary"
      (response.json \ "annualTaxSummaryLink" \ "destination").as[String] shouldBe "SA"
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

      val response = await(getRequestWithAcceptHeader(url))

      (response.json \ "annualTaxSummaryLink").isEmpty shouldBe true
    }
  }
}
