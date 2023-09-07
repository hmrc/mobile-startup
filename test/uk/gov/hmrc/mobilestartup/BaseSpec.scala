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

package uk.gov.hmrc.mobilestartup

import org.scalamock.scalatest.MockFactory
import org.scalatest.OptionValues
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilestartup.model.types.ModelTypes.JourneyId
import eu.timepit.refined.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

trait BaseSpec
    extends AnyWordSpecLike
    with Matchers
    with MockFactory
    with FutureAwaits
    with DefaultAwaitTimeout
    with OptionValues {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  val journeyId:        JourneyId     = "7f1b5289-5f4d-4150-93a3-ff02dda28375"

}
