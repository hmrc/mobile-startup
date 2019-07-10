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
import javax.inject.{Inject, Named}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.service.Auditor

import scala.concurrent.{ExecutionContext, Future}

class LivePreFlightService @Inject()(
  genericConnector:                                    GenericConnector[Future],
  val auditConnector:                                  AuditConnector,
  val authConnector:                                   AuthConnector,
  @Named("appName") val appName:                       String,
  @Named("controllers.confidenceLevel") val confLevel: Int
)(
  implicit executionContext: ExecutionContext
) extends PreFlightServiceImpl[Future](genericConnector, confLevel)
    with AuthorisedFunctions
    with Auditor {

  // Just adapting from `F` to `Future` here
  override def auditing[T](service: String, details: Map[String, String])(f: => Future[T])(implicit hc: HeaderCarrier): Future[T] =
    withAudit(service, details)(f)

  // The retrieval function is really hard to dummy out in tests because of it's polymorphic nature, and the `~` trick doesn't
  // help, but isolating it here and adapting to the concrete tuple of results we are expecting makes testing of the logic in
  // `PreFlightServiceImpl` much easier.
  override def retrieveAccounts(implicit hc: HeaderCarrier): Future[(Option[Nino], Option[SaUtr], Credentials, ConfidenceLevel)] =
    authConnector.authorise(EmptyPredicate, nino and saUtr and credentials and confidenceLevel).map {
      case foundNino ~ foundSaUtr ~ creds ~ conf =>
        (foundNino.map(Nino(_)), foundSaUtr.map(SaUtr(_)), creds, conf)
    }
}
