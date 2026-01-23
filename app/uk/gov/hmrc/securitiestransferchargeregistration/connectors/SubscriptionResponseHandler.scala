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
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.securitiestransferchargeregistration.models.SubscriptionResponse

import scala.concurrent.Future

class SubscriptionErrorException(msg: String) extends RuntimeException(msg)

class SubscriptionResponseParseError(msg: String) extends RuntimeException(msg)


object SubscriptionResponseHandler extends Logging {

  def handle(response: HttpResponse): Future[String] = {
    val body = response.body
    response.status match {
      case CREATED =>
        Json.parse(body).validate[SubscriptionResponse].fold(
          errors => {
            val msg = s"[SubscriptionResponseHandler] Invalid success response JSON received. Validation errors: $errors. Body: $body"
            logger.error(msg)
            Future.failed(SubscriptionResponseParseError(msg))
          },
          success =>
            Future.successful(success.success.stcId)
        )
      case other =>
        val msg = s"[SubscriptionResponseHandler] Subscription request failed with HTTP status $other and body: $body"
        logger.error(msg)
        Future.failed(SubscriptionErrorException(msg))
    }
  }
}
