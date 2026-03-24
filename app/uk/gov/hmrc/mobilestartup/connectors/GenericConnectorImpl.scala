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

import javax.inject.Inject
import play.api.{Configuration, Logging}
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.mobilestartup.model.{CidPerson, EnrolmentStoreResponse, PertaxResponse}
import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

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
    json:         Option[JsValue] = None,
    serviceName:  String,
    path:         String,
    hc:           HeaderCarrier
  )(implicit rds: HttpReads[T]
  ): F[PertaxResponse]
}

@Singleton
class GenericConnectorImpl @Inject() (
  configuration: Configuration,
  wSHttp:        HttpClientV2
)(implicit ec:   ExecutionContext)
    extends GenericConnector[Future]
    with Logging {

  def protocol(serviceName: String): String =
    getServiceConfig(serviceName).getOptional[String]("protocol").getOrElse("https")

  def host(serviceName: String): String = getConfigProperty(serviceName, "host")

  def port(serviceName: String): Int = getConfigProperty(serviceName, "port").toInt

  def http: HttpClientV2 & HttpClientV2 = wSHttp

  def doGet(
    serviceName: String,
    path:        String,
    hc:          HeaderCarrier
  ): Future[JsValue] = {
    implicit val hcHeaders: HeaderCarrier = addAPIHeaders(hc)
    val url = buildUrl(protocol(serviceName), host(serviceName), port(serviceName), path)
    http.get(url"$url").execute[JsValue]
  }

  def cidGet(
    serviceName: String,
    path:        String,
    hc:          HeaderCarrier
  ): Future[CidPerson] = {
    implicit val hcHeaders: HeaderCarrier = addAPIHeaders(hc)
    val url = buildUrl(protocol(serviceName), host(serviceName), port(serviceName), path)
    http.get(url"$url").execute[HttpResponse].map { response =>
      if (response.status == NOT_FOUND || response.status == NO_CONTENT)
        throw new NotFoundException("No UTR found for user")
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
    http.get(url"$url").execute[HttpResponse].map { response =>
      if (response.status == 204) EnrolmentStoreResponse(Seq.empty)
      else response.json.as[EnrolmentStoreResponse]
    }
  }

  def doPost[T](
    json:         Option[JsValue] = None,
    serviceName:  String,
    path:         String,
    hc:           HeaderCarrier
  )(implicit rds: HttpReads[T]
  ): Future[PertaxResponse] = {
    implicit val hcHeaders: HeaderCarrier = addAPIHeaders(hc)
    val url = buildUrl(protocol(serviceName), host(serviceName), port(serviceName), path)
    val postResponse: Future[PertaxResponse] = json match {
      case Some(jsBody) => http.post(url"$url").withBody(jsBody).execute[PertaxResponse]
      case None =>
        http
          .post(url"$url")
          .setHeader(HeaderNames.ACCEPT -> "application/vnd.hmrc.2.0+json")
          .execute[PertaxResponse]
    }
    postResponse.recover {
      case UpstreamErrorResponse(_, status, _, _) if status == 401 =>
        logger.info(s"pertax authorise call failed with unauthorised exception")
        throw UnauthorizedException(s" User is unauthorised")
      case UpstreamErrorResponse(_, status, _, _) if status == 499 =>
        logger.info(s"pertax authorise call failed with BadGatewayException")
        throw BadGatewayException(s"Dependant services failing")
      case ex: Exception =>
        logger.info(s"pertax authorise call failed with exception ")
        throw InternalServerException(s"Unexpected response from pertax api")
    }

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
