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

package uk.gov.hmrc.mobilestartup.controllers

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.test.FakeRequest
import uk.gov.hmrc.mobilestartup.BaseSpec

trait BaseControllerSpec extends BaseSpec {
  val iOSVersionJson:             JsValue              = toJson(iOSVersion)
  val acceptJsonHeader:           (String, String)     = "Accept" -> "application/vnd.hmrc.1.0+json"
  val iOSRequest:                 FakeRequest[JsValue] = FakeRequest().withBody(iOSVersionJson)
  val iOSRequestWithValidHeaders: FakeRequest[JsValue] = FakeRequest().withBody(iOSVersionJson).withHeaders(acceptJsonHeader)
  val upgradeNotRequiredResult = s"""{"upgradeRequired":false}"""
  val upgradeRequiredResult    = s"""{"upgradeRequired":true}"""
}
