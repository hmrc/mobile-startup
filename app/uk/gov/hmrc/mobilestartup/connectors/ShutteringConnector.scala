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

package uk.gov.hmrc.mobilestartup.connectors

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.mobilestartup.model.shuttering.{Shuttering, StartupShuttering}
import uk.gov.hmrc.mobilestartup.model.types.JourneyId

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShutteringConnector @Inject() (
  http:                                   HttpClientV2,
  @Named("mobile-shuttering") serviceUrl: String) {

  val logger: Logger = Logger(this.getClass)

  def getStartupShuttering(
    journeyId:              JourneyId
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[StartupShuttering] =
    for {
      npsShuttered          <- getShutteringStatus("mobile-startup-citizen-details", journeyId)
      childBenefitShuttered <- getShutteringStatus("mobile-startup-child-benefit", journeyId)
    } yield (StartupShuttering(npsShuttered, childBenefitShuttered))

  private def getShutteringStatus(
    service:                String,
    journeyId:              JourneyId
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[Shuttering] =
    http
      .get(url"$serviceUrl/mobile-shuttering/service/$service/shuttered-status?journeyId=$journeyId")
      .execute[Shuttering]
      .recover {
        case e =>
          logger.warn(s"Call to mobile-shuttering failed:\n $e \nAssuming unshuttered.")
          Shuttering.shutteringDisabled
      }
}
