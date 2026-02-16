import sbt._

object AppDependencies {

  private val catsVersion     = "2.13.0"
  private val bootstrapVersion = "10.5.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % bootstrapVersion,
    "org.typelevel"           %% "cats-core"                    % catsVersion,
    "org.apache.pekko"        %% "pekko-connectors-xml"         % "1.2.0",
    "io.lemonlabs"            %% "scala-uri"                    % "4.0.3",
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-30"  % "2.4.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "3.1.0",
    "uk.gov.hmrc"             %% "crypto-json-play-30"          % "8.4.0",
    "org.apache.pekko"        %% "pekko-protobuf-v3"            % "1.2.1",
    "org.apache.pekko"        %% "pekko-serialization-jackson"  % "1.2.1",
    "org.apache.pekko"        %% "pekko-stream"                 % "1.2.1",
    "org.apache.pekko"        %% "pekko-actor-typed"            % "1.2.1"
  )

  val test = Seq(
    "org.apache.pekko"    %% "pekko-testkit"          % "1.2.1",
    "org.scalatest"       %% "scalatest"              % "3.2.19",
    "uk.gov.hmrc"         %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.typelevel"       %% "cats-core"              % catsVersion,
    "com.vladsch.flexmark" % "flexmark-all"           % "0.64.8",
    "org.mockito"          % "mockito-core"           % "5.20.0",
    "org.scalatestplus"   %% "mockito-5-12"           % "3.2.19.0",
    "org.scalacheck"      %% "scalacheck"             % "1.19.0",
    "org.scalatestplus"   %% "scalacheck-1-18"        % "3.2.19.0",
    "org.scalatestplus"   %% "scalacheck-1-18"        % "3.2.19.0"
  ).map(_ % Test)
}
