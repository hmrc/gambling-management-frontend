import uk.gov.hmrc.DefaultBuildSettings
import play.sbt.routes.RoutesKeys

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
    ".*ViewUtils.*",
    ".*controllers\\.routes\\..*",
    ".*views\\.html\\..*",
    ".*target/scala-3.*/routes/.*"
  ).mkString(";")

ThisBuild / coverageMinimumStmtTotal := 76
ThisBuild / coverageFailOnMinimum := true
ThisBuild / coverageHighlighting := true

// --------------------------------------------

lazy val root = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(DefaultBuildSettings.defaultSettings(): _*)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:msg=unused import&src=html/.*:s",
    pipelineStages := Seq(gzip),
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    coverageExcludedPackages := (ThisBuild / coverageExcludedPackages).value,
    coverageExcludedFiles := (ThisBuild / coverageExcludedFiles).value,
    retrieveManaged := true,
    pipelineStages := Seq(digest),
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Wconf:src=html/.*:s",
      "-Wconf:src=routes/.*:s",
      "-Wconf:src=target/.*:s",
      "-Wconf:msg=unused import:s",
      "-Wconf:msg=Flag.*repeatedly:s"
    ),
    PlayKeys.playDefaultPort := 10400,
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "controllers.routes._",
      "viewmodels.govuk.all._"
    )
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
