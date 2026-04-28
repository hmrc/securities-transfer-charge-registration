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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}

object EtmpErrorResponseHelper extends Results with Logging {

  def badRequestFromError(error: ErrorDetails): Result =
    BadRequest(Json.obj(
      "code" -> error.code,
      "message" -> error.message,
      "logID" -> error.logID
    ))

  def unprocessableEntityFromError(error: ValidationErrorDetails): Result =
    UnprocessableEntity(Json.obj(
      "processingDate" -> error.processingDate,
      "code" -> error.code,
      "text" -> error.text
    ))

  def logUnexpectedError(operation: String, ex: Throwable): Unit =
    logger.error(s"Unexpected error calling ETMP $operation", ex)  
}
