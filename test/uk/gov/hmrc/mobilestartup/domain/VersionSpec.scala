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
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class VersionSpec extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks with NoShrink {
  import VersionGenerators._

  import scala.language.implicitConversions
  private implicit def toLeft(i:  Int):    Left[Int, Nothing]     = Left(i)
  private implicit def toRight(s: String): Right[Nothing, String] = Right(s)

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfig(minSuccessful = 5000)

  "Version parsing" should "correctly parse a single-digit positive version number" in {
    forAll(noShrink(Gen.posNum[Int]), noShrink(Gen.option(genBoq))) {
      case (NoShrinkWrapper(i), NoShrinkWrapper(boq)) =>
        val stringVersion = (List(s"$i") ++ formatBoq(boq)).mkString("-")
        Version.fromString(stringVersion) shouldBe Version(i, 0, 0, boq)
    }
  }

  it should "correctly parse two-component version numbers, where at least one component is greater than zero" in {
    forAll(noShrink(genPairWithANonZero), noShrink(Gen.option(genBoq))) {
      case (NoShrinkWrapper(vs), NoShrinkWrapper(boq)) =>
        val versionString = (List(s"${vs._1}.${vs._2}") ++ formatBoq(boq)).mkString("-")
        Version.fromString(versionString) shouldBe Version(vs._1, vs._2, 0, boq)
    }
  }

  it should "correctly parse three-component version numbers, where at least one component is greater than zero" in {
    forAll(noShrink(genTripleWithANonZero), noShrink(Gen.option(genBoq))) {
      case (NoShrinkWrapper(vs), NoShrinkWrapper(boq)) =>
        val versionString = (List(s"${vs._1}.${vs._2}.${vs._3}") ++ formatBoq(boq)).mkString("-")
        Version.fromString(versionString) shouldBe Version(vs._1, vs._2, vs._3, boq)
    }
  }


  it should "work for '1.2.3-alpha-1'" in {
    Version.fromString("1.2.3-alpha-1") shouldBe Version(1, 2, 3, Some("alpha-1"))
  }
  it should "work for '1.2-alpha-1-20050205.060708-1'" in {
    Version.fromString("1.2-alpha-1-20050205.060708-1") shouldBe Version(1, 2, 0, Some("alpha-1-20050205.060708-1"))
  }
  it should "work for '999-SNAPSHOT'" in {
    Version.fromString("999-SNAPSHOT") shouldBe Version(999, 0, 0, Some("SNAPSHOT"))
  }
  it should "work for '2.0-1'" in {
    Version.fromString("2.0-1") shouldBe Version(2, 0, 0, Some(Left(1)))
  }
  it should "work for '1.2.3-RC4'" in {
    Version.fromString("1.2.3-RC-4") shouldBe Version(1, 2, 3, Some("RC-4"))
  }

  "Unsupported version schemas" should "transformed into qualifiers" in {
    Version.fromString("1.0.1b")                shouldBe Version(0, 0, 0, Some("1.0.1b"))
    Version.fromString("1.0M2")                 shouldBe Version(0, 0, 0, Some("1.0M2"))
    Version.fromString("1.0RC2")                shouldBe Version(0, 0, 0, Some("1.0RC2"))
    Version.fromString("1.7.3.0")               shouldBe Version(0, 0, 0, Some("1.7.3.0"))
    Version.fromString("1.7.3.0-1")             shouldBe Version(0, 0, 0, Some("1.7.3.0-1"))
    Version.fromString("PATCH-1193602")         shouldBe Version(0, 0, 0, Some("PATCH-1193602"))
    Version.fromString("5.0.0alpha-2006020117") shouldBe Version(0, 0, 0, Some("5.0.0alpha-2006020117"))
    Version.fromString("1.0.0.-SNAPSHOT")       shouldBe Version(0, 0, 0, Some("1.0.0.-SNAPSHOT"))
    Version.fromString("1..0-SNAPSHOT")         shouldBe Version(0, 0, 0, Some("1..0-SNAPSHOT"))
    Version.fromString("1.0.-SNAPSHOT")         shouldBe Version(0, 0, 0, Some("1.0.-SNAPSHOT"))
    Version.fromString(".1.0-SNAPSHOT")         shouldBe Version(0, 0, 0, Some(".1.0-SNAPSHOT"))
  }

  "1.0.0" should "be greater than 0.0.1" in {
    Version.fromString("1.0.0").isAfter(Version.fromString("0.0.1")) shouldBe true
  }

  "2.11.2" should "be greater than 2.9.3" in {
    Version.fromString("2.11.2").isAfter(Version.fromString("2.9.3")) shouldBe true
  }

  "1.0.0" should "be smaller than 1.0.1" in {
    Version.fromString("1.0.0").isBefore(Version.fromString("1.0.1")) shouldBe true
  }

  "1.0.1" should "be equal to 1.0.1" in {
    Version.fromString("1.0.1").equals(Version.fromString("1.0.1")) shouldBe true
  }

  "1.0.1" should "not be equal to 1.0.1-SNAPSHOT" in {
    Version.fromString("1.0.1").equals(Version.fromString("1.0.1-SNAPSHOT")) shouldBe false
  }

  "1.0.1" should "be after to 1.0.1-SNAPSHOT" in {
    Version.fromString("1.0.1").isAfter(Version.fromString("1.0.1-SNAPSHOT")) shouldBe true
  }

  "Version comparison" should "work for '1.0-alpha-1' < 1.0" in {
    Version.fromString("1.0-alpha-1").isBefore(Version.fromString("1.0")) shouldBe true
  }

  it should "work for '1.0-alpha-1' < 1.0-alpha-2" in {
    Version.fromString("1.0-alpha-1").isBefore(Version.fromString("1.0-alpha-2")) shouldBe true
  }

  it should "work for '1.0-alpha-1' < 1.0-beta-2" in {
    Version.fromString("1.0-alpha-1").isBefore(Version.fromString("1.0-beta-1")) shouldBe true
  }

  it should "work for '1.0-alpha-2' < 1.0-alpha-15" in {
    Version.fromString("1.0-alpha-2").isBefore(Version.fromString("1.0-beta-15")) shouldBe true
  }

  it should "work for '1.0-alpha-1' < 1.0-beta-1" in {
    Version.fromString("1.0-alpha-1").isBefore(Version.fromString("1.0-beta-1")) shouldBe true
  }

  it should "work for '1.0-SNAPSHOT' < 1.0-beta-1" in {
    Version.fromString("1.0-SNAPSHOT").isBefore(Version.fromString("1.0-beta-1")) shouldBe true
  }

  it should "work for '1.0-SNAPSHOT' < 1.0" in {
    Version.fromString("1.0-SNAPSHOT").isBefore(Version.fromString("1.0")) shouldBe true
  }

  it should "work for '1.0-alpha-1-SNAPSHOT' < 1.0-alpha-2" in {
    Version.fromString("1.0-alpha-1-SNAPSHOT").isBefore(Version.fromString("1.0-alpha-2")) shouldBe true
  }

  it should "work for '1.0' < 1.0-1" in {
    Version.fromString("1.0").isBefore(Version.fromString("1.0-1")) shouldBe true
  }
  it should "work for 1.0-1 < 1.0-2" in {
    Version.fromString("1.0-1").isBefore(Version.fromString("1.0-2")) shouldBe true
  }
  it should "work for 2.0 < 2.0-1" in {
    Version.fromString("2.0").isBefore(Version.fromString("2.0-1")) shouldBe true
  }
  it should "work for 2.0.0 < 2.0-1" in {
    Version.fromString("2.0.0").isBefore(Version.fromString("2.0-1")) shouldBe true
  }
  it should "work for 2.0-1 < 2.0.1" in {
    Version.fromString("2.0-1").isBefore(Version.fromString("2.0.1")) shouldBe true
  }
  it should "work for 2.0.1-klm < 2.0.1-lmn" in {
    Version.fromString("2.0.1-klm").isBefore(Version.fromString("2.0.1-lmn")) shouldBe true
  }
  it should "work for 2.0.1 < 2.0.1-123" in {
    Version.fromString("2.0.1").isBefore(Version.fromString("2.0.1-123")) shouldBe true
  }
  it should "work for 2.0.1-xyz < 2.0.1-123" in {
    Version.fromString("2.0.1-xyz").isBefore(Version.fromString("2.0.1-123")) shouldBe true
  }
  it should "work for '1.2.3-10000000000' < 1.2.3-10000000001" in {
    Version.fromString("1.2.3-10000000000").isBefore(Version.fromString("1.2.3-10000000001")) shouldBe true
  }
  it should "work for '1.2.3-1' < 1.2.3-10000000001" in {
    Version.fromString("1.2.3-1").isBefore(Version.fromString("1.2.3-10000000001")) shouldBe true
  }
  it should "work for '2.3.0-v200706262000' < 2.3.0-v200706262130" in {
    Version.fromString("2.3.0-v200706262000").isBefore(Version.fromString("2.3.0-v200706262130")) shouldBe true
  }
  it should "work for '2.0.0.v200706041905-7C78EK9E_EkMNfNOd2d8qq' < 2.0.0.v200706041906-7C78EK9E_EkMNfNOd2d8qq" in {
    Version
      .fromString("2.0.0.v200706041905-7C78EK9E_EkMNfNOd2d8qq")
      .isBefore(Version.fromString("2.0.0.v200706041906-7C78EK9E_EkMNfNOd2d8qq")) shouldBe true
  }

  "Version" should "recognise an early release" in {
    Version.isSnapshot(Version(2, 2, 3, Some(Right("SNAPSHOT")))) shouldBe true
    Version.isSnapshot(Version(2, 2, 2))                          shouldBe false
  }

  it should "recognise '*-SNAP1' as a snapshot" in {
    Version.isSnapshot(Version(2, 2, 3, Some(Right("M1")))) shouldBe true
  }

  it should "recognise '*-M1' as a snapshot" in {
    Version.isSnapshot(Version(2, 2, 3, Some(Right("M1")))) shouldBe true
  }

  it should "recognise '*-FINAL' as a release" in {
    Version.isSnapshot(Version(2, 2, 3, Some(Right("FINAL")))) shouldBe false
  }

  it should "recognise '2.3.0_0.1.8' as a release" in {
    Version.isSnapshot(Version.fromString("2.3.0_0.1.8")) shouldBe false
  }

  it should "correctly print qualifiers" in {
    Version.fromString("2.0.1-klm").toString shouldBe "2.0.1-klm"
  }
}
