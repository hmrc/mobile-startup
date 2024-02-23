import play.core.PlayVersion
import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "7.20.0"
  private val domainVersion        = "8.1.0-play-28"
  private val playHmrcApiVersion   = "7.2.0-play-28"
  private val taxYearVersion       = "3.0.0"

  private val pegdownVersion    = "1.6.0"
  private val refinedVersion    = "0.9.26"
  private val wireMockVersion   = "2.21.0"
  private val catsCoreVersion   = "2.9.0"
  private val scalaMockVersion  = "5.1.0"
  private val scalaCheckVersion = "3.2.3.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc"   %% "play-hmrc-api"             % playHmrcApiVersion,
    "uk.gov.hmrc"   %% "tax-year"                  % taxYearVersion,
    "uk.gov.hmrc"   %% "domain"                    % domainVersion,
    "org.typelevel" %% "cats-core"                 % catsCoreVersion,
    "eu.timepit"    %% "refined"                   % refinedVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq(
    "org.pegdown" % "pegdown"                 % pegdownVersion       % scope,
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapPlayVersion % scope
  )

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock"     %% "scalamock"       % scalaMockVersion  % scope,
            "org.scalatestplus" %% "scalacheck-1-15" % scalaCheckVersion % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % "it"
          )
      }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
