import play.core.PlayVersion
import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "3.0.0"
  private val scalaTestPlusVersion = "3.1.2"
  private val domainVersion        = "5.6.0-play-26"
  private val playHmrcApiVersion   = "5.3.0-play-26"
  private val taxYearVersion       = "1.0.0"

  private val scalaTestVersion              = "3.0.8"
  private val pegdownVersion                = "1.6.0"
  private val refinedVersion                = "0.9.4"
  private val wireMockVersion               = "2.21.0"
  private val catsCoreVersion               = "2.1.0"
  private val scalaMockVersion              = "4.1.0"
  private val scalaCheckVersion             = "1.14.0"
  private val serviceIntegrationTestVersion = "0.9.0-play-26"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-26" % bootstrapPlayVersion,
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
    "org.pegdown"            % "pegdown"             % pegdownVersion       % scope,
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current  % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope
  )

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock"  %% "scalamock"  % scalaMockVersion  % scope,
            "org.scalacheck" %% "scalacheck" % scalaCheckVersion % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalatest"          %% "scalatest"                % scalaTestVersion              % "it",
            "uk.gov.hmrc"            %% "service-integration-test" % serviceIntegrationTestVersion % "it",
            "com.github.tomakehurst" % "wiremock-jre8"             % wireMockVersion               % "it"
          )
      }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
