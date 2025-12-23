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

import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import uk.gov.hmrc.securitiestransferchargeregistration.models._
import uk.gov.hmrc.securitiestransferchargeregistration.services.RegistrationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationController @Inject()(
                                        cc: ControllerComponents,
                                        registrationService: RegistrationService
                                      )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def registerIndividual: Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      request.body.validate[IndividualRegistrationDetails].fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        details =>
          registrationService.registerIndividual(details).map {
            case RegistrationFlowSuccess =>
              Ok(Json.obj("status" -> "registered"))

            case RegistrationFlowFailure(reason) =>
              InternalServerError(Json.obj("error" -> reason))
          }
      )
    }
}

