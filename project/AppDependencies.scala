import sbt._

object AppDependencies {

  private val catsVersion     = "2.13.0"
  private val bootstrapVersion = "10.7.0"
  private val pekkoVersion = "1.5.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % bootstrapVersion,
    "org.typelevel"           %% "cats-core"                    % catsVersion,
    "org.apache.pekko"        %% "pekko-connectors-xml"         % "1.3.0",
    "io.lemonlabs"            %% "scala-uri"                    % "4.0.3",
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-30"  % "2.5.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "4.3.0",
    "uk.gov.hmrc"             %% "crypto-json-play-30"          % "8.4.0",
    "org.apache.pekko"        %% "pekko-protobuf-v3"            % pekkoVersion,
    "org.apache.pekko"        %% "pekko-serialization-jackson"  % pekkoVersion,
    "org.apache.pekko"        %% "pekko-stream"                 % pekkoVersion,
    "org.apache.pekko"        %% "pekko-actor-typed"            % pekkoVersion
  )

  val test = Seq(
    "org.apache.pekko"    %% "pekko-testkit"          % pekkoVersion,
    "org.scalatest"       %% "scalatest"              % "3.2.20",
    "uk.gov.hmrc"         %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.typelevel"       %% "cats-core"              % catsVersion,
    "com.vladsch.flexmark" % "flexmark-all"           % "0.64.8",
    "org.mockito"          % "mockito-core"           % "5.23.0",
    "org.scalatestplus"   %% "mockito-5-12"           % "3.2.19.0",
    "org.scalacheck"      %% "scalacheck"             % "1.19.0",
    "org.scalatestplus"   %% "scalacheck-1-18"        % "3.2.19.0",
    "org.scalatestplus"   %% "scalacheck-1-18"        % "3.2.19.0"
  ).map(_ % Test)
}
