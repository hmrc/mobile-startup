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
import cats._
import scala.util.{Failure, Success, Try}

object TestFInstances {
  type TestF[A] = Try[A]

  implicit val F: MonadError[TestF, Throwable] =
    new MonadError[TestF, Throwable] {
      def pure[A](x: A): Try[A] = Success(x)

      def flatMap[A, B](fa: Try[A])(f: A => Try[B]): Try[B] =
        fa.flatMap(f)

      def tailRecM[A, B](a: A)(f: A => Try[Either[A, B]]): Try[B] =
        f(a) match {
          case Success(Right(b))    => Success(b)
          case Success(Left(nextA)) => tailRecM(nextA)(f)
          case Failure(e)           => util.Failure(e)
        }

      def raiseError[A](e: Throwable): Try[A] = Failure(e)

      def handleErrorWith[A](fa: Try[A])(f: Throwable => Try[A]): Try[A] =
        fa.recoverWith { case t => f(t) }
    }

  implicit class ThrowableOps(t: Throwable) {
    def error[A]: Try[A] = Failure(t)
  }

  implicit class TryOps[A](v: Try[A]) {
    def unsafeGet: A = v.get
  }
}
