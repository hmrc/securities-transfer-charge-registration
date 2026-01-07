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

package uk.gov.hmrc.securitiestransferchargeregistration.services

import uk.gov.hmrc.securitiestransferchargeregistration.connectors.{EacdClient, EtmpClient}
import uk.gov.hmrc.securitiestransferchargeregistration.models.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject()(
                                     etmpClient: EtmpClient,
                                     eacdClient: EacdClient
                                   )(implicit ec: ExecutionContext) {

  def registerIndividual(details: IndividualRegistrationDetails): Future[RegistrationFlowResult] =
    etmpClient
      .register(details)
      .map(safeId => RegistrationFlowSuccess(safeId))
      .recover { case e => RegistrationFlowFailure(e.getMessage) }

  def subscribeIndividual(details: IndividualSubscriptionDetails): Future[SubscriptionFlowResult] =
    etmpClient
      .subscribeIndividual(details)
      .map(subscriptionId => SubscriptionFlowSuccess(subscriptionId))
      .recover { case e => SubscriptionFlowFailure(e.getMessage) }

  def subscribeOrganisation(details: OrganisationSubscriptionDetails): Future[SubscriptionFlowResult] =
    etmpClient
      .subscribeOrganisation(details)
      .map(subscriptionId => SubscriptionFlowSuccess(subscriptionId))
      .recover { case e => SubscriptionFlowFailure(e.getMessage) }

  def enrolIndividual(details: IndividualEnrolmentDetails): Future[EnrolmentFlowResult] =
    eacdClient
      .enrolIndividual(details)
      .map(_ => EnrolmentFlowSuccess)
      .recover { case e => EnrolmentFlowFailure(e.getMessage) }

  def hasCurrentSubscription(etmpSafeId: String): Future[SubscriptionStatusFlowResult] =
    etmpClient
      .hasCurrentSubscription(etmpSafeId)
      .map {
        case true  => SubscriptionStatusActive
        case false => SubscriptionStatusNotFound
      }
      .recover { case e => SubscriptionStatusFailure(e.getMessage) }
}