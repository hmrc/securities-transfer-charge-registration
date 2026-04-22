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

sealed trait StcSubscriptionCreateResponse

object StcSubscriptionCreateResponse {


  final case class SuccessResponse(
                                    success: SubscriptionSuccess
                                  ) extends StcSubscriptionCreateResponse

  final case class SubscriptionSuccess(
                                   processingDate: String,
                                   stcId: String
                                 )

  implicit val subscriptionSuccessFormat: OFormat[SubscriptionSuccess] = Json.format[SubscriptionSuccess]
  implicit val successResponseFormat: OFormat[SuccessResponse] = Json.format[SuccessResponse]
  
  final case class BadRequestResponse(
                                       error: ErrorDetails
                                     ) extends StcSubscriptionCreateResponse


  implicit val badRequestResponseFormat: OFormat[BadRequestResponse] = Json.format[BadRequestResponse]
  
  final case class UnprocessableEntityResponse(
                                                errors: ValidationErrorDetails
                                              ) extends StcSubscriptionCreateResponse
  
  implicit val unprocessableEntityResponseFormat: OFormat[UnprocessableEntityResponse] = Json.format[UnprocessableEntityResponse]
  
  def fromHttpResponse(response: HttpResponse): StcSubscriptionCreateResponse =
    response.status match
      case 201 =>
        response.json.validate[SuccessResponse] match
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw UpstreamErrorResponse(
              s"Unable to parse ETMP subscription create 201 response: $errors",
              502,
              502,
              response.headers
            )

      case 400 =>
        response.json.validate[BadRequestResponse] match
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw UpstreamErrorResponse(
              s"Unable to parse ETMP subscription create 400 response: $errors",
              502,
              502,
              response.headers
            )

      case 422 =>
        response.json.validate[UnprocessableEntityResponse] match
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw UpstreamErrorResponse(
              s"Unable to parse ETMP subscription create 422 response: $errors",
              502,
              502,
              response.headers
            )

      case status =>
        throw UpstreamErrorResponse(
          s"Unexpected ETMP subscription create response status: $status",
          status,
          status,
          response.headers
        )
}

