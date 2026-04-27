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

final case class IndividualSubscriptionDetails(
                                                override val safeId: String,
                                                override val contactName: String,
                                                override val addressLine1: String,
                                                override val addressLine2: Option[String],
                                                override val addressLine3: Option[String],
                                                override val postcode: String,
                                                override val countryCode: String,
                                                override val telephoneNumber: String,
                                                override val email: String
                                              ) extends SubscriptionDetails(safeId, contactName, addressLine1, addressLine2, addressLine3, postcode, countryCode, telephoneNumber, email)

object IndividualSubscriptionDetails {
  implicit val format: OFormat[IndividualSubscriptionDetails] = Json.format[IndividualSubscriptionDetails]
}
