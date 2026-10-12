package uk.gov.hmrc.securitiestransferchargeregistration.models

import uk.gov.hmrc.securitiestransferchargeregistration.support.ISpecBase

class SubscriptionDetailsSpec extends ISpecBase {

  "SubscriptionDetails" should {

    "truncate long postcodes" in {
      val longPostcode = "ThisIsTooLongForAPostcode"
      val expectedPostcode = "ThisIsTooL"

      val underTest = SubscriptionDetails(
        safeId = "safe",
        contactName = "Fred",
        addressLine1 = "1 High Street",
        addressLine2 = Some("Bobbins upon sea"),
        addressLine3 = None,
        postcode = longPostcode,
        countryCode = "GB",
        telephoneNumber = "01234567890",
        email = "fred@bobbins.com"
      )

      underTest.postcode mustBe expectedPostcode
    }
  }

  "preserve valid postcodes" in {
    val shortPostcode = "NE12 6YT"

    val underTest = SubscriptionDetails(
      safeId = "safe",
      contactName = "Fred",
      addressLine1 = "1 High Street",
      addressLine2 = Some("Bobbins upon sea"),
      addressLine3 = None,
      postcode = shortPostcode,
      countryCode = "GB",
      telephoneNumber = "01234567890",
      email = "fred@bobbins.com"
    )

    underTest.postcode mustBe shortPostcode
  }
}
