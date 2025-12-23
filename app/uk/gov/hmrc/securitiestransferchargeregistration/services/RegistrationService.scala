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

import uk.gov.hmrc.securitiestransferchargeregistration.connectors.EtmpClient
import uk.gov.hmrc.securitiestransferchargeregistration.models.{IndividualRegistrationDetails, RegistrationFlowFailure, RegistrationFlowResult, RegistrationFlowSuccess}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject()(
                                     etmpClient: EtmpClient
                                   )(implicit ec: ExecutionContext) {

  def registerIndividual(
                          details: IndividualRegistrationDetails
                        ): Future[RegistrationFlowResult] = {

    for {
      _ <- etmpClient.register(details)
      _ <- etmpClient.subscribeIndividual(details.nino)
      _ <- etmpClient.enrolIndividual(details.nino)
    } yield RegistrationFlowSuccess
  }.recover {
    case e => RegistrationFlowFailure(e.getMessage)
  }
}

