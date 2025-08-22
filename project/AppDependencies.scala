import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "10.1.0"
  private val domainVersion        = "12.1.0"
  private val playHmrcApiVersion   = "8.3.0"
  private val taxYearVersion       = "6.0.0"
  private val refinedVersion        = "0.11.3"
  private val catsCoreVersion      = "2.13.0"
  private val scalaMockVersion     = "7.4.1"
  private val scalaCheckVersion    = "3.2.18.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc"   %% "play-hmrc-api-play-30"     % playHmrcApiVersion,
    "uk.gov.hmrc"   %% "tax-year"                  % taxYearVersion,
    "uk.gov.hmrc"   %% "domain-play-30"            % domainVersion,
    "org.typelevel" %% "cats-core"                 % catsCoreVersion,
    "eu.timepit"    %% "refined"                   % refinedVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % scope
  )

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock"     %% "scalamock"       % scalaMockVersion  % scope,
            "org.scalatestplus" %% "scalacheck-1-17" % scalaCheckVersion % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope)
      }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
