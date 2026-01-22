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

package models.subscription


import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.securitiestransferchargeregistration.models.{IndividualSubscriptionDetails, OrganisationSubscriptionDetails, Subscription}

class SubscriptionSpec extends AnyWordSpec with Matchers {

  "Subscription.fromIndividual" should {
    "map all fields correctly" in {
      val individualSubscriptionDetails = IndividualSubscriptionDetails(
        safeId = "SAFE-123",
        contactName="Test Name",
        addressLine1 = "1 Test Street",
        addressLine2 = Some("Area"),
        addressLine3 = None,
        postCode = "ZZ1 1ZZ",
        country = "GB",
        telephoneNumber = "0123456789",
        email = "ind@example.com"
      )

      val subscription = Subscription.fromIndividual(individualSubscriptionDetails)

      subscription.contactName mustBe individualSubscriptionDetails.contactName
      subscription.addressLine1 mustBe individualSubscriptionDetails.addressLine1
      subscription.addressLine2 mustBe individualSubscriptionDetails.addressLine2
      subscription.addressLine3 mustBe individualSubscriptionDetails.addressLine3
      subscription.postcode mustBe individualSubscriptionDetails.postCode
      subscription.countryCode mustBe individualSubscriptionDetails.country
      subscription.telephoneNumber mustBe individualSubscriptionDetails.telephoneNumber
      subscription.email mustBe individualSubscriptionDetails.email

      subscription.mobileNumber mustBe None
    }

  }

  "Subscription.fromOrganisation" should {
    "map all fields correctly" in {
      val organisationSubscriptionDetails = OrganisationSubscriptionDetails(
        safeId = "SAFE-123",
        addressLine1 = "350 But Close",
        addressLine2 = Some("Greenwich"),
        addressLine3 = Some("London"),
        postCode = "SE10 7KGT",
        country = "GB",
        telephoneNumber = "02081129921",
        email = "org@example.com"
      )

      val subscription = Subscription.fromOrganisation(organisationSubscriptionDetails)

      subscription.contactName mustBe ""
      subscription.addressLine1 mustBe subscription.addressLine1
      subscription.addressLine2 mustBe subscription.addressLine2
      subscription.addressLine3 mustBe subscription.addressLine3
      subscription.postcode mustBe subscription.postcode
      subscription.countryCode mustBe subscription.countryCode
      subscription.telephoneNumber mustBe subscription.telephoneNumber
      subscription.email mustBe subscription.email
      subscription.mobileNumber mustBe None
    }
  }

  "Subscription JSON format" should {
    "serializes and deserializes with optional fields present" in {
      val model = Subscription(
        contactName = "",
        addressLine1 = "350 But Close",
        addressLine2 = Some("Greenwich"),
        addressLine3 = Some("London"),
        postcode = "SE10 7KGT",
        countryCode = "GB",
        telephoneNumber = "02081129921",
        mobileNumber = Some("07988211282"),
        email = "jdoe@example.com"
      )

      val json = Json.toJson(model)
      json.validate[Subscription].asOpt mustBe Some(model)

    }

    "serializes and deserializes correctly when option fields are None" in {
      val model = Subscription(
        contactName = "",
        addressLine1 = "1 Test Street",
        addressLine2 = None,
        addressLine3 = None,
        postcode = "ZZ1 1ZZ",
        countryCode = "GB",
        telephoneNumber = "0123456789",
        mobileNumber = None,
        email = "ind@example.com"
      )

      val json = Json.toJson(model)
      json.validate[Subscription].asOpt mustBe Some(model)
      
    }
  }
}
