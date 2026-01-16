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

final case class EtmpIndividual(firstName: String, lastName: String, dateOfBirth: Option[String])
object EtmpIndividual { implicit val format: OFormat[EtmpIndividual] = Json.format[EtmpIndividual] }

final case class EtmpRegistrationRequest(
                                          regime: String,
                                          requiresNameMatch: Boolean,
                                          isAnAgent: Boolean,
                                          individual: EtmpIndividual
                                        )
object EtmpRegistrationRequest { implicit val format: OFormat[EtmpRegistrationRequest] = Json.format[EtmpRegistrationRequest] }

final case class EtmpRegistrationSuccessResponse(safeId: String)
object EtmpRegistrationSuccessResponse { implicit val format: OFormat[EtmpRegistrationSuccessResponse] = Json.format[EtmpRegistrationSuccessResponse] }

final case class EtmpFailureResponse(code: String, reason: String)
object EtmpFailureResponse { implicit val format: OFormat[EtmpFailureResponse] = Json.format[EtmpFailureResponse] }

final case class EtmpSubscribeSuccessResponse(subscriptionId: String)
object EtmpSubscribeSuccessResponse:
  given OFormat[EtmpSubscribeSuccessResponse] = Json.format[EtmpSubscribeSuccessResponse]