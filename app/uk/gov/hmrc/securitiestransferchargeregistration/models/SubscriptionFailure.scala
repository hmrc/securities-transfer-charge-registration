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


sealed trait SubscriptionFailure

object SubscriptionFailure {

  case object Unauthorized extends SubscriptionFailure
  case object Forbidden extends SubscriptionFailure
  case object NotFound extends SubscriptionFailure
  
  final case class InvalidSuccessResponse(body: String) extends SubscriptionFailure
  final case class InvalidErrorResponse(status: Int, body: String) extends SubscriptionFailure

  final case class UnexpectedStatus(status: Int, body: String) extends SubscriptionFailure
}

