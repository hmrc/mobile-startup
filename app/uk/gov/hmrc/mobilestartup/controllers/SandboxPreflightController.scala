package uk.gov.hmrc.mobilestartup.controllers
import java.util.UUID.randomUUID

import javax.inject.Inject
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilestartup.services.PreFlightCheckResponse
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

class SandboxPreflightController @Inject()(
  val controllerComponents: ControllerComponents
)(
  implicit val executionContext: ExecutionContext
) extends BackendBaseController
    with FileResource
    with HeaderValidator {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  def preFlightCheck(journeyId: Option[String]): Action[DeviceVersion] =
    validateAccept(acceptHeaderValidationRules).async(controllerComponents.parsers.json[DeviceVersion]) { implicit request =>
      val sandboxControl: Option[String] = request.headers.get("SANDBOX-CONTROL")

      Future.successful {
        sandboxControl match {
          case Some("ERROR-401") => Unauthorized
          case Some("ERROR-403") => Forbidden
          case Some("ERROR-500") => InternalServerError
          case _ =>
            val (upgrade, toIV, routeToTwoFactor) = sandboxControl match {
              case Some("UPGRADE-REQUIRED")    => (true, false, false)
              case Some("ROUTE-TO-IV")         => (false, true, false)
              case Some("ROUTE-TO-TWO-FACTOR") => (false, false, true)
              case _                           => (false, false, false)
            }

            Ok(toJson(buildPreFlightResponse(upgrade, toIV, routeToTwoFactor)))
        }
      }
    }

  def buildPreFlightResponse(upgrade: Boolean, toIV: Boolean, twoFactor: Boolean): PreFlightCheckResponse =
    PreFlightCheckResponse(upgradeRequired = upgrade, Accounts(Some(Nino("CS700100A")), None, toIV, randomUUID().toString))
}
