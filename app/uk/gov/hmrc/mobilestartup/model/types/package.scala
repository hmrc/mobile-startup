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

import eu.timepit.refined.api.{RefType, Refined, Validate}
import eu.timepit.refined.refineV
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsSuccess, Reads, Writes}
import play.api.mvc.QueryStringBindable

package object types {

  implicit def refinedReads[T, P](
    implicit reads: Reads[T],
    validate:       Validate[T, P]
  ): Reads[T Refined P] =
    Reads[T Refined P] { json =>
      reads
        .reads(json)
        .flatMap { t: T =>
          refineV[P](t) match {
            case Left(error)  => JsError(error)
            case Right(value) => JsSuccess(value)
          }
        }
    }

  implicit def refinedWrites[T, P](implicit writes: Writes[T]): Writes[T Refined P] = writes.contramap(_.value)

  implicit def refinedQueryStringBindable[R[_, _], T, P](
    implicit
    baseTypeBinder: QueryStringBindable[T],
    refType:        RefType[R],
    validate:       Validate[T, P]
  ): QueryStringBindable[R[T, P]] = new QueryStringBindable[R[T, P]] {

    override def bind(
      key:    String,
      params: Map[String, Seq[String]]
    ): Option[Either[String, R[T, P]]] =
      baseTypeBinder
        .bind(key, params)
        .map(_.right.flatMap { baseValue =>
          refType.refine[P](baseValue)
        })

    override def unbind(
      key:   String,
      value: R[T, P]
    ): String =
      baseTypeBinder.unbind(key, refType.unwrap(value))
  }

}
