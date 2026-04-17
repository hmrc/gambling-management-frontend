import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.7.0"
  private val hmrcMongoVersion = "2.12.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "13.3.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % hmrcMongoVersion
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
    "org.jsoup"          % "jsoup"                   % "1.13.1"         % Test,
    "org.scalacheck"    %% "scalacheck"              % "1.19.0"         % Test,
    "org.scalatestplus" %% "scalacheck-1-19"         % "3.2.19.0"       % Test,
    "org.scalamock"     %% "scalamock"               % "7.5.5"          % Test

  )

  val it = Seq.empty
}
