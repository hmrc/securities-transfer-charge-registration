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

import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.securitiestransferchargeregistration.models.{SubscriptionFailure, SubscriptionResponse}

object SubscriptionResponseHandler extends Logging {

  def handle(response: HttpResponse): Either[SubscriptionFailure, String] =
    response.status match {
      case 200 =>
        parseSuccess(response.body)

      case 400 =>
        logger.error(
          s"[SubscriptionResponseHandler] Bad Request (400) with body: ${response.body}"
        )
        Left(SubscriptionFailure.InvalidErrorResponse(response.status, response.body))

      case 401 =>
        logger.warn("[SubscriptionResponseHandler] Unauthorized (401)")
        Left(SubscriptionFailure.Unauthorized)
      case 403 =>
        logger.warn("[SubscriptionResponseHandler] Forbidden (403)")
        Left(SubscriptionFailure.Forbidden)
      case 404 =>
        logger.warn("[SubscriptionResponseHandler] Not found (404)")
        Left(SubscriptionFailure.NotFound)

      case other =>
        logger.error(
          s"[SubscriptionResponseHandler] Unexpected status $other with body: ${response.body}"
        )
        Left(SubscriptionFailure.UnexpectedStatus(other, response.body))
    }

  private def parseSuccess(body: String): Either[SubscriptionFailure, String] =
    Json.parse(body)
      .validate[SubscriptionResponse]
      .asEither
      .left
      .map { error =>
        logger.error(
          s"[SubscriptionResponseHandler] failed to parse success response JSON: $error, body: $body"
        )
        SubscriptionFailure.InvalidSuccessResponse(body)
      }
      .map(_.success.stcId)
}
