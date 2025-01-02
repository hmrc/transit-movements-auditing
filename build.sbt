import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings

val appName = "transit-movements-auditing"
ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.4.3"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9498,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.transitmovementsauditing.models.Binders._",
      "uk.gov.hmrc.transitmovementsauditing.models._"
    )
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(inThisBuild(buildSettings))

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)

// Settings for the whole build
lazy val buildSettings = Def.settings(
  scalafmtOnCompile := false
)
