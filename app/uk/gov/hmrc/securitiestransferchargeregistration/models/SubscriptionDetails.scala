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

import play.api.libs.json.{JsValue, Json, OFormat}

case class SubscriptionDetails(
                                safeId: String,
                                contactName: String,
                                addressLine1: String,
                                addressLine2: Option[String],
                                addressLine3: Option[String],
                                postcode: String,
                                countryCode: String,
                                telephoneNumber: String,
                                email: String
                              ) {
  def toSubscription: Subscription =
    Subscription(
      contactName = contactName,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      postcode = postcode,
      countryCode = countryCode,
      telephoneNumber = telephoneNumber,
      email = email
    )
}

object SubscriptionDetails {
  
  implicit  val format: OFormat[SubscriptionDetails] = Json.format[SubscriptionDetails]
}
