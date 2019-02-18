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

package uk.gov.hmrc.mobilestartup.domain
import org.scalacheck.Gen

import scala.util.Random

trait NoShrink {
  case class NoShrinkWrapper[T](value: T)

  def noShrink[T](gen: Gen[T]): Gen[NoShrinkWrapper[T]] = gen.map(NoShrinkWrapper.apply)
}

object VersionGenerators {
  def genEither[A, B](genA: Gen[A], genB: Gen[B]): Gen[Either[A, B]] =
    if (Random.nextBoolean()) genA.map(Left(_)) else genB.map(Right(_))

  val genZeroOrGreater: Gen[Int] = Gen.choose(0, Int.MaxValue)

  def formatBoq(boq: Option[Either[Long, String]]): Option[String] = boq.map {
    case Left(num) => num.toString
    case Right(st) => st
  }

  // Make sure the value is a) non-empty and b) isn't made up of all digits, otherwise it would
  // get interpreted as a numeric boq value when parsing the version string
  val genStringBoqValue: Gen[String] = for {
    a <- Gen.alphaChar
    s <- Gen.alphaNumStr
  } yield a + s

  val genBoq: Gen[Either[Long, String]] =
    genEither(Gen.chooseNum[Long](0, Long.MaxValue), genStringBoqValue)

  val genIntPair: Gen[(Int, Int)] = {
    for {
      m <- genZeroOrGreater
      n <- genZeroOrGreater
    } yield (m, n)
  }

  // Need a pair of ints, but at least one of them needs to be non-zero
  val genPairWithANonZero: Gen[(Int, Int)] = genIntPair.flatMap {
    case (0, 0) =>
      // Can't have them both zero, so randomly replace one of the pair values with a positive number
      if (Random.nextBoolean()) Gen.posNum[Int].map((_, 0)) else Gen.posNum[Int].map((0, _))
    case pair => Gen.const(pair)
  }

  val genIntTriple: Gen[(Int, Int, Int)] = {
    for {
      m <- genZeroOrGreater
      n <- genZeroOrGreater
      o <- genZeroOrGreater
    } yield (m, n, o)
  }

  // Need a pair of ints, but at least one of them needs to be non-zero
  val genTripleWithANonZero: Gen[(Int, Int, Int)] = genIntTriple.flatMap {
    case (0, 0, 0) =>
      Random.nextInt(2) match {
        case 0 => Gen.posNum[Int].map((_, 0, 0))
        case 1 => Gen.posNum[Int].map((0, _, 0))
        case 2 => Gen.posNum[Int].map((0, 0, _))
      }

    case vs => Gen.const(vs)
  }
}
