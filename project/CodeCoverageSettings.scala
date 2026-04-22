import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "uk.gov.hmrc.securitiestransferchargeregistration.models.*",
    "uk.gov.hmrc.securitiestransferchargeregistration.connectors.StcSubscriptionAmendResponse.*",
    "uk.gov.hmrc.securitiestransferchargeregistration.connectors.StcSubscriptionCreateResponse.*",
    "uk.gov.hmrc.securitiestransferchargeregistration.connectors.StcSubscriptionViewResponse.*"
  )

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 100,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
