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


import play.api.libs.json.{Json, OFormat}

case class Subscription(
                         contactName: String,
                         addressLine1: String,
                         addressLine2: Option[String],
                         addressLine3: Option[String],
                         postcode: String,
                         countryCode: String,
                         telephoneNumber: String,
                         mobileNumber: Option[String] = None,
                         email: String
                       )

object Subscription {

  def fromIndividual(details: IndividualSubscriptionDetails): Subscription =
    Subscription(
      contactName = details.contactName,
      addressLine1 = details.addressLine1,
      addressLine2 = details.addressLine2,
      addressLine3 = details.addressLine3,
      postcode = details.postCode,
      countryCode = details.country,
      telephoneNumber = details.telephoneNumber,
      email = details.email
    )

  def fromOrganisation(details: OrganisationSubscriptionDetails): Subscription =
    Subscription(
      contactName = "", //Todo need to update to confirm where the name is coming from
      addressLine1 = details.addressLine1,
      addressLine2 = details.addressLine2,
      addressLine3 = details.addressLine3,
      postcode = details.postCode,
      countryCode = details.country,
      telephoneNumber = details.telephoneNumber,
      email = details.email
    )
    
  implicit val format: OFormat[Subscription] = Json.format[Subscription]
}


