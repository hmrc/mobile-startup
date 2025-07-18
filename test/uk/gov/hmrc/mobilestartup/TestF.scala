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

package uk.gov.hmrc.mobilestartup

import cats.MonadError
import cats.instances.try_._

import scala.util.{Failure, Try}

/**
  * Defines a type constructor that can be used in tests to instantiate components that have a type constructor
  * parameter. All tests can just use `TestF` to construct the services, and `F` to generate values (e.g.
  * `F.pure(a)` or `F.raiseError(t)`
  */
import cats.MonadError
import scala.util.{Try, Failure}

trait TestF {
  type TestF[A] = Try[A]

  implicit val F: MonadError[TestF, Throwable] = MonadError[TestF, Throwable]

  implicit class ErrorSyntax(t: Throwable) {
    def error[A]: Try[A] = Failure(t)
  }

  implicit class ValueSyntax[A](v: Try[A]) {
    def unsafeGet: A = v.get
  }
}
