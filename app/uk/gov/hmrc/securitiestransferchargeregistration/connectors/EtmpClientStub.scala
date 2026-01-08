/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.securitiestransferchargeregistration.models.{IndividualRegistrationDetails, IndividualSubscriptionDetails, OrganisationSubscriptionDetails, SubscriptionStatusActive, SubscriptionStatusFlowResult}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class EtmpClientStub @Inject() extends EtmpClient {

  override def register(details: IndividualRegistrationDetails): Future[String] =
    Future.successful("SAFE123")

  override def subscribeIndividual(details: IndividualSubscriptionDetails): Future[String] =
    Future.successful("SUBSCRIPTION123")

  override def subscribeOrganisation(details: OrganisationSubscriptionDetails): Future[String] =
    Future.successful("SUBSCRIPTION123")

  override def hasCurrentSubscription(etmpSafeId: String): Future[SubscriptionStatusFlowResult] =
    Future.successful(SubscriptionStatusActive)
}

