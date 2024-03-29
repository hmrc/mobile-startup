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

package uk.gov.hmrc.mobilestartup.model

import play.api.libs.json.Json.format
import play.api.libs.json.OFormat

object Person {
  implicit val formats: OFormat[Person] = format[Person]
}

case class Person(
  firstName:  Option[String],
  middleName: Option[String],
  lastName:   Option[String]) {

  lazy val shortName: String =
    List(firstName, middleName, lastName).flatten.mkString(" ")
}

object Address {
  implicit val formats: OFormat[Address] = format[Address]
}

case class Address(
  line1:    Option[String] = None,
  line2:    Option[String] = None,
  line3:    Option[String] = None,
  line4:    Option[String] = None,
  line5:    Option[String] = None,
  postcode: Option[String] = None,
  country:  Option[String] = None)

object PersonDetails {
  implicit val formats: OFormat[PersonDetails] = format[PersonDetails]
}

case class PersonDetails(
  person:  Person,
  address: Option[Address])
