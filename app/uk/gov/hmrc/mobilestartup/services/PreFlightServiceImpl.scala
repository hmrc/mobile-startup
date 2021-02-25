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
import cats.MonadError
import cats.implicits._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ItmpName}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, UnsupportedAuthProvider}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId

abstract class PreFlightServiceImpl[F[_]](
  genericConnector:       GenericConnector[F],
  minimumConfidenceLevel: Int
)(implicit F:             MonadError[F, Throwable])
    extends PreFlightService[F] {

  // The authentication and auditing calls from the platform are based on Future so declare a couple of
  // methods that adapt away from Future to F that the live implementation can define.
  def retrieveAccounts(
    implicit hc: HeaderCarrier
  ): F[(Option[Nino], Option[SaUtr], Option[Credentials], ConfidenceLevel, Option[ItmpName], Option[AnnualTaxSummaryLink])]

  def auditing[T](
    service:     String,
    details:     Map[String, String]
  )(f:           => F[T]
  )(implicit hc: HeaderCarrier
  ): F[T]

  def preFlight(journeyId: JourneyId)(implicit hc: HeaderCarrier): F[PreFlightCheckResponse] =
    auditing("preFlightCheck", Map.empty) {
      getPreFlightCheckResponse(journeyId)
    }

  private def getPreFlightCheckResponse(journeyId: JourneyId)(implicit hc: HeaderCarrier): F[PreFlightCheckResponse] =
    retrieveAccounts.map {
      case (nino, saUtr, credentials, confidenceLevel, name, annualTaxSummaryLink) =>
        if (credentials.getOrElse(Credentials("Unsupported", "Unsupported")).providerType != "GovernmentGateway")
          throw new UnsupportedAuthProvider
        PreFlightCheckResponse(
          nino,
          saUtr,
          minimumConfidenceLevel > confidenceLevel.level,
          name,
          annualTaxSummaryLink
        )
    }

}
