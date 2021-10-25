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

package uk.gov.hmrc.mobilestartup.model

import play.api.libs.json.{Format, JsResult, JsString, JsSuccess, JsValue}

sealed trait EnrolmentStatus {
  val link: Option[String]
}

object EnrolmentStatus {

  implicit val format: Format[EnrolmentStatus] = new Format[EnrolmentStatus] {

    override def writes(status: EnrolmentStatus): JsValue = status match {
      case Activated       => JsString("activated")
      case NoEnrolment     => JsString("noEnrolment")
      case NotYetActivated => JsString("notYetActivated")
      case WrongAccount    => JsString("wrongAccount")
      case NoUtr           => JsString("noUtr")
    }

    override def reads(json: JsValue): JsResult[EnrolmentStatus] = json.as[String] match {
      case "activated"       => JsSuccess(Activated)
      case "noEnrolment"     => JsSuccess(NoEnrolment)
      case "notYetActivated" => JsSuccess(NotYetActivated)
      case "wrongAccount"    => JsSuccess(WrongAccount)
      case "noUtr"           => JsSuccess(NoUtr)
    }
  }
}

case object Activated extends EnrolmentStatus {
  val link: Option[String] = None
}

case object NoEnrolment extends EnrolmentStatus {
  val link: Option[String] = Some("/personal-account/sa-enrolment")
}

case object NotYetActivated extends EnrolmentStatus {
  val link: Option[String] = Some("/personal-account/self-assessment")
}

case object WrongAccount extends EnrolmentStatus {
  val link: Option[String] = Some("/personal-account/self-assessment")
}

case object NoUtr extends EnrolmentStatus {
  val link: Option[String] = Some("https://www.gov.uk/register-for-self-assessment")
}
