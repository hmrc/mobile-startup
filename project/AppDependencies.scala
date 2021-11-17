import play.core.PlayVersion
import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "5.12.0"
  private val scalaTestPlusVersion = "5.1.0"
  private val domainVersion        = "6.2.0-play-28"
  private val playHmrcApiVersion   = "6.4.0-play-28"
  private val taxYearVersion       = "1.0.0"

  private val scalaTestVersion              = "3.2.3"
  private val pegdownVersion                = "1.6.0"
  private val refinedVersion                = "0.9.4"
  private val wireMockVersion               = "2.21.0"
  private val catsCoreVersion               = "2.1.0"
  private val scalaMockVersion              = "4.1.0"
  private val scalaCheckVersion             = "3.2.3.0"
  private val serviceIntegrationTestVersion = "1.2.0-play-28"
  private val flexmarkAllVersion            = "0.36.8"

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
    "org.pegdown"            % "pegdown"             % pegdownVersion       % scope,
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current  % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "com.vladsch.flexmark"   % "flexmark-all"        % flexmarkAllVersion   % scope
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
            "org.scalatest"          %% "scalatest"                % scalaTestVersion              % "it",
            "uk.gov.hmrc"            %% "service-integration-test" % serviceIntegrationTestVersion % "it",
            "com.github.tomakehurst" % "wiremock-jre8"             % wireMockVersion               % "it"
          )
      }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
