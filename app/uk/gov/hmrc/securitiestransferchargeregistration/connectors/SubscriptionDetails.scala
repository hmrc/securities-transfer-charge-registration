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

package uk.gov.hmrc.securitiestransferchargeregistration.connectors

import play.api.libs.json.{JsValue, Json, OFormat, OWrites, Reads, Writes}
import uk.gov.hmrc.securitiestransferchargeregistration.models.Subscription

sealed trait SubscriptionDetails {
  def safeId: String

  def addressLine1: String

  def addressLine2: Option[String]

  def addressLine3: Option[String]

  def postcode: String

  def countryCode: String

  def telephoneNumber: String

  def email: String
}

final case class IndividualSubscriptionDetails(
                                                safeId: String,
                                                contactName: String,
                                                addressLine1: String,
                                                addressLine2: Option[String] = None,
                                                addressLine3: Option[String] = None,
                                                postcode: String,
                                                countryCode: String,
                                                telephoneNumber: String,
                                                email: String
                                              ) extends SubscriptionDetails

final case class OrganisationSubscriptionDetails(
                                                  safeId: String,
                                                  addressLine1: String,
                                                  addressLine2: Option[String] = None,
                                                  addressLine3: Option[String] = None,
                                                  postcode: String,
                                                  countryCode: String,
                                                  telephoneNumber: String,
                                                  mobileNumber: Option[String] = None,
                                                  email: String
                                                ) extends SubscriptionDetails


object SubscriptionDetails {

  implicit val individualFormat: OFormat[IndividualSubscriptionDetails] =
    Json.format[IndividualSubscriptionDetails]

  implicit val organisationFormat: OFormat[OrganisationSubscriptionDetails] =
    Json.format[OrganisationSubscriptionDetails]

  def toJson(details: SubscriptionDetails): JsValue =
    details match {
      case i: IndividualSubscriptionDetails =>
        val sub = Subscription(contactName = i.contactName,
          addressLine1 = i.addressLine1,
          addressLine2 = i.addressLine2,
          addressLine3 = i.addressLine3,
          postcode = i.postcode,
          countryCode = i.countryCode,
          telephoneNumber = i.telephoneNumber,
          email = i.email)
        Json.toJson(sub)
      case o: OrganisationSubscriptionDetails =>
        val sub = Subscription(contactName = "",
          addressLine1 = o.addressLine1,
          addressLine2 = o.addressLine2,
          addressLine3 = o.addressLine3,
          postcode = o.postcode,
          countryCode = o.countryCode,
          telephoneNumber = o.telephoneNumber,
          email = o.email)
        Json.toJson(sub)
    }

}
