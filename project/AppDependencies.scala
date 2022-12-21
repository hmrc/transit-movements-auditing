import sbt._

object AppDependencies {

  private val catsVersion     = "2.8.0"
  private val hmrcPlayVersion = "7.12.0"

  val compile = Seq(
    "uk.gov.hmrc"        %% "bootstrap-backend-play-28" % hmrcPlayVersion,
    "org.typelevel"      %% "cats-core"                 % catsVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-xml"   % "3.0.4",
    "io.lemonlabs"       %% "scala-uri"                 % "3.6.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % hmrcPlayVersion,
    "org.mockito"             % "mockito-core"            % "4.5.1",
    "org.mockito"            %% "mockito-scala-scalatest" % "1.17.5",
    "org.typelevel"          %% "cats-core"               % catsVersion,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.36.8",
    "org.scalacheck"         %% "scalacheck"              % "1.15.4",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "4.0.3",
    "org.scalatestplus"      %% "mockito-3-2"             % "3.1.2.0",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.2.0"
  ).map(_ % "test, it")
}
