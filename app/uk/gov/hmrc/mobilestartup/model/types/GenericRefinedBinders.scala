/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.mobilestartup.model.types

import play.api.mvc.{PathBindable, QueryStringBindable}

object GenericRefinedBinders {

  def queryStringBindable[T](from: String => Either[String, T])(to: T => String): QueryStringBindable[T] =
    new QueryStringBindable[T] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] =
        params.get(key).flatMap(_.headOption).map(from)

      override def unbind(key: String, value: T): String = to(value)
    }

  def pathBindable[T](from: String => Either[String, T])(to: T => String): PathBindable[T] =
    new PathBindable[T] {
      override def bind(key: String, value: String): Either[String, T] = from(value)

      override def unbind(key: String, value: T): String = to(value)
    }
}
