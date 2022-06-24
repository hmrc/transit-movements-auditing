import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val catsVersion     = "2.7.0"
  private val hmrcPlayVersion = "5.24.0"

  val compile = Seq(
    "uk.gov.hmrc"        %% "bootstrap-backend-play-28" % hmrcPlayVersion,
    "org.typelevel"      %% "cats-core"                 % catsVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-xml"   % "3.0.4"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % hmrcPlayVersion % "test, it",
    "org.mockito"             % "mockito-core"            % "4.5.1",
    "org.mockito"            %% "mockito-scala-scalatest" % "1.17.5",
    "org.typelevel"          %% "cats-core"               % catsVersion,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.36.8"        % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "4.0.3",
    "org.scalatestplus"      %% "mockito-3-2"             % "3.1.2.0"
  )
}
