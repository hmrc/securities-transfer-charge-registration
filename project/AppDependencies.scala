import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.7.0"
  

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "7.0.2"                     % Test,
    "org.mockito"             % "mockito-core"                % "5.23.0"                    % Test
    
  )

  val it: Seq[Nothing] = Seq.empty
}
