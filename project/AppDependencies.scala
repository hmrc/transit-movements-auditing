import sbt._

object AppDependencies {

  private val catsVersion     = "2.9.0"
  private val hmrcPlayVersion = "8.4.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % hmrcPlayVersion,
    "org.typelevel"           %% "cats-core"                    % catsVersion,
    "org.apache.pekko"        %% "pekko-connectors-xml"         % "1.0.1",
    "io.lemonlabs"            %% "scala-uri"                    % "3.6.0",
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-30"  % "1.3.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "1.8.0"
  )

  val test = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-30"  % hmrcPlayVersion,
    "org.typelevel"       %% "cats-core"               % catsVersion,
    "com.vladsch.flexmark" % "flexmark-all"            % "0.36.8",
    "org.scalacheck"      %% "scalacheck"              % "1.16.0",
    "org.mockito"         %% "mockito-scala-scalatest" % "1.17.14",
    "org.scalatestplus"   %% "mockito-3-2"             % "3.1.2.0",
    "org.scalatestplus"   %% "scalacheck-1-15"         % "3.2.2.0"
  ).map(_ % Test)
}
