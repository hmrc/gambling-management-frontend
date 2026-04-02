import uk.gov.hmrc.DefaultBuildSettings
import scoverage.ScoverageKeys

lazy val appName: String = "gambling-management-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"
ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s"

ThisBuild / coverageExcludedPackages :=
  Seq(
    "router\\..*",
    "controllers\\.routes\\..*",
    "views.html\\..*",
    "config\\..*",
    "testOnly\\..*",
    "controllers.testOnly\\..*",
    "forms\\.validation\\.mappings",
    "partials\\..*",
    "testOnlyDoNotUseInAppConf\\..*",
    "uk.gov.hmrc.BuildInfo"
  ).mkString(";")

ThisBuild / coverageExcludedFiles :=
  Seq(
    ".*Routes\\.scala",
    ".*RoutesPrefix\\.scala",
    ".*Reverse.*",
    ".*controllers\\.routes\\..*",
    ".*views\\.html\\..*",
    ".*target/scala-3.*/routes/.*"
  ).mkString(";")

ThisBuild / coverageMinimumStmtTotal := 76
ThisBuild / coverageFailOnMinimum := true
ThisBuild / coverageHighlighting := true

// --------------------------------------------

lazy val root = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:msg=unused import&src=html/.*:s",
    pipelineStages := Seq(gzip),
    coverageExcludedPackages := (ThisBuild / coverageExcludedPackages).value,
    coverageExcludedFiles := (ThisBuild / coverageExcludedFiles).value
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
  .settings(coverageEnabled := false)

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)