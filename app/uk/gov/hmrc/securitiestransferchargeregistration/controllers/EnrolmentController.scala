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

package uk.gov.hmrc.securitiestransferchargeregistration.controllers

import play.api.libs.json.{JsError, JsValue}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregistration.models.*
import uk.gov.hmrc.securitiestransferchargeregistration.services.RegistrationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentController @Inject()(
                                     cc: ControllerComponents,
                                     registrationService: RegistrationService
                                   )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def enrolIndividual: Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      request.body.validate[IndividualEnrolmentDetails].fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        details =>
          registrationService.enrolIndividual(details).map {
            case EnrolmentFlowSuccess => NoContent
            case EnrolmentFlowFailure(_) => InternalServerError
          }
      )
    }

  def enrolOrganisation: Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      request.body.validate[OrganisationEnrolmentDetails].fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        details =>
          registrationService.enrolOrganisation(details).map {
            case EnrolmentFlowSuccess => NoContent
            case EnrolmentFlowFailure(_) => InternalServerError
          }
      )
    }
}