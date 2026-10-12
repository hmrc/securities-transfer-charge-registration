/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.securitiestransferchargeregistration.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers.should

class SubscriptionDetailsSpec extends AnyFreeSpec {

  "SubscriptionDetails should" - {

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
