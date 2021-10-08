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

package uk.gov.hmrc.mobilestartup.mocks

import org.scalamock.scalatest.MockFactory
import play.api.libs.json.Json
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.mobilestartup.model.CidPerson

import scala.concurrent.{ExecutionContext, Future}

trait CitizenDetailsMock extends MockFactory {

//  val http: CoreGet = mock[CoreGet]
//
//  def mockCitizenDetailsCall(response: CidPerson) =
//    (
//      http
//        .GET("/citizen-details/nino/CS700100A", _: Seq[(String, String)], _: Seq[(String, String)])(
//          _: HttpReads[HttpResponse],
//          _: HeaderCarrier,
//          _: ExecutionContext
//        )
//      )
//      .expects(*, *, *, *, *)
//      .returns(Future successful HttpResponse(200, Json.toJson(response), Map.empty))

}
