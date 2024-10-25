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

package uk.gov.hmrc.mobilestartup.connectors

import com.google.inject.Singleton
import eu.timepit.refined.util.string.url

import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.mobilestartup.model.{CidPerson, EnrolmentStoreResponse}
import play.api.http.Status.{NOT_FOUND, NO_CONTENT}

import scala.concurrent.{ExecutionContext, Future}

trait GenericConnector[F[_]] {

  def doGet(
    serviceName: String,
    path:        String,
    hc:          HeaderCarrier
  ): F[JsValue]

  def cidGet(
    serviceName: String,
    path:        String,
    hc:          HeaderCarrier
  ): F[CidPerson]

  def enrolmentStoreGet(
    serviceName: String,
    path:        String,
    hc:          HeaderCarrier
  ): F[EnrolmentStoreResponse]

  def doPost[T](
    json:         JsValue,
    serviceName:  String,
    path:         String,
    hc:           HeaderCarrier
  )(implicit rds: HttpReads[T]
  ): F[T]
}

@Singleton
class GenericConnectorImpl @Inject() (
  configuration: Configuration,
  wSHttp:        HttpClientV2
)(implicit ec:   ExecutionContext)
    extends GenericConnector[Future] {

  def protocol(serviceName: String): String =
    getServiceConfig(serviceName).getOptional[String]("protocol").getOrElse("https")

  def host(serviceName: String): String = getConfigProperty(serviceName, "host")

  def port(serviceName: String): Int = getConfigProperty(serviceName, "port").toInt

  def http: HttpClientV2 with HttpClientV2 = wSHttp

  def doGet(
    serviceName: String,
    path:        String,
    hc:          HeaderCarrier
  ): Future[JsValue] = {
    implicit val hcHeaders: HeaderCarrier = addAPIHeaders(hc)
    val url = buildUrl(protocol(serviceName),host(serviceName),port(serviceName),path)
    http.get(url"$url").execute[JsValue]
  }

  def cidGet(
    serviceName: String,
    path:        String,
    hc:          HeaderCarrier
  ): Future[CidPerson] = {
    implicit val hcHeaders: HeaderCarrier = addAPIHeaders(hc)
    val url = buildUrl(protocol(serviceName), host(serviceName), port(serviceName), path)
    http.get(url"$url").execute[HttpResponse].map {
      response =>
        if (response.status == NOT_FOUND || response.status == NO_CONTENT) throw new NotFoundException("No UTR found for user")
        else response.json.as[CidPerson]
    }
  }

  def enrolmentStoreGet(
    serviceName: String,
    path:        String,
    hc:          HeaderCarrier
  ): Future[EnrolmentStoreResponse] = {
    implicit val hcHeaders: HeaderCarrier = addAPIHeaders(hc)
    val url = buildUrl(protocol(serviceName), host(serviceName), port(serviceName), path)
    http.get(url"$url").execute[HttpResponse].map {
      response =>
        if (response.status == 204) EnrolmentStoreResponse(Seq.empty)
        else response.json.as[EnrolmentStoreResponse]
    }
  }

  def doPost[T](
    json:         JsValue,
    serviceName:  String,
    path:         String,
    hc:           HeaderCarrier
  )(implicit rds: HttpReads[T]
  ): Future[T] = {
    implicit val hcHeaders: HeaderCarrier = addAPIHeaders(hc)
    val url = buildUrl(protocol(serviceName), host(serviceName), port(serviceName), path)
    http.post(url"$url").withBody(json).execute[T]
  }

  private def addAPIHeaders(hc: HeaderCarrier): HeaderCarrier =
    hc.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  private def buildUrl(
    protocol: String,
    host:     String,
    port:     Int,
    path:     String
  ): String = s"""$protocol://$host:$port$path"""

  private def getConfigProperty(
    serviceName: String,
    property:    String
  ): String =
    getServiceConfig(serviceName)
      .getOptional[String](property)
      .getOrElse(throw new Exception(s"No service configuration found for $serviceName"))

  private def getServiceConfig(serviceName: String): Configuration =
    configuration
      .getOptional[Configuration](s"microservice.services.$serviceName")
      .getOrElse(throw new Exception(s"No micro services configured for $serviceName"))
}
