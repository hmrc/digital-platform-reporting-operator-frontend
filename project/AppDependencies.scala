import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.9.0"
  private val hmrcMongoVersion = "2.5.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"    % "11.11.0",
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"            % hmrcMongoVersion,
    "com.googlecode.libphonenumber"  % "libphonenumber"                % "8.13.55",
    "com.beachape"                  %% "enumeratum-play"               % "1.8.2",
    "org.typelevel"                 %% "cats-core"                     % "2.13.0",
    "uk.gov.hmrc"                   %% "crypto-json-play-30"           % "8.2.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.18.0",
    "org.mockito"             %% "mockito-scala"           % "1.17.37"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
