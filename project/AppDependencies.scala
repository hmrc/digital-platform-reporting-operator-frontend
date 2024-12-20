import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.3.0"
  private val hmrcMongoVersion = "2.2.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"    % "10.8.0",
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"            % hmrcMongoVersion,
    "com.googlecode.libphonenumber"  % "libphonenumber"                % "8.13.42",
    "com.beachape"                  %% "enumeratum-play"               % "1.8.1",
    "org.typelevel"                 %% "cats-core"                     % "2.12.0",
    "uk.gov.hmrc"                   %% "crypto-json-play-30"           % "8.1.0",
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.17.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
