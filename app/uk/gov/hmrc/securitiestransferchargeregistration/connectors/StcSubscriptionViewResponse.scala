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

import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.securitiestransferchargeregistration.models.{ErrorDetails, ValidationErrorDetails}

sealed trait StcSubscriptionViewResponse

object StcSubscriptionViewResponse {

  final case class SuccessResponse(
                                    success: SubscriptionDetails
                                  ) extends StcSubscriptionViewResponse

  final case class SubscriptionDetails(
                                   processingDate: String,
                                   subsValidTo: String,
                                   contactName: String,
                                   addressLine1: String,
                                   addressLine2: Option[String],
                                   addressLine3: Option[String],
                                   postcode: String,
                                   countryCode: String,
                                   telephoneNumber: String,
                                   mobileNumber: Option[String],
                                   email: String
                                 )

  implicit val successDetailsFormat: OFormat[SubscriptionDetails] = Json.format[SubscriptionDetails]
  implicit val successResponseFormat: OFormat[SuccessResponse] = Json.format[SuccessResponse]

  
  final case class BadRequestResponse(
                                       error: ErrorDetails
                                     ) extends StcSubscriptionViewResponse


  implicit val badRequestResponseFormat: OFormat[BadRequestResponse] = Json.format[BadRequestResponse]

  
  final case class UnprocessableEntityResponse(
                                                errors: ValidationErrorDetails
                                              ) extends StcSubscriptionViewResponse


  implicit val unprocessableEntityResponseFormat: OFormat[UnprocessableEntityResponse] = Json.format[UnprocessableEntityResponse]


  def fromHttpResponse(response: HttpResponse): StcSubscriptionViewResponse =
    response.status match
      case 200 =>
        response.json.validate[SuccessResponse] match
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw UpstreamErrorResponse(
              s"Unable to parse ETMP subscription view 200 response: $errors",
              502,
              502,
              response.headers
            )

      case 400 =>
        response.json.validate[BadRequestResponse] match
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw UpstreamErrorResponse(
              s"Unable to parse ETMP subscription view 400 response: $errors",
              502,
              502,
              response.headers
            )

      case 422 =>
        response.json.validate[UnprocessableEntityResponse] match
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw UpstreamErrorResponse(
              s"Unable to parse ETMP subscription view 422 response: $errors",
              502,
              502,
              response.headers
            )

      case status =>
        throw UpstreamErrorResponse(
          s"Unexpected ETMP subscription view response status: $status",
          status,
          status,
          response.headers
        )
}
