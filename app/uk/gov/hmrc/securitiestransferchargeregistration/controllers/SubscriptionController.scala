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
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregistration.connectors.*
import uk.gov.hmrc.securitiestransferchargeregistration.models.{ErrorMessages, EtmpErrorResponseHelper, Subscription, SubscriptionDetails}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionController @Inject()(
                                        cc: ControllerComponents,
                                        etmpClient: EtmpClient,
                                      )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  private def headerValue(request: RequestHeader, name: String): Option[String] =
    request.headers.get(name).map(_.trim).filter(_.nonEmpty)

  def createSubscription: Action[JsValue] = Action.async(parse.json) { implicit request =>

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    headerValue(request, "correlation-id") match {

      case None => Future.successful(BadRequest(Json.obj("message" -> ErrorMessages.MissingCorrelationId)))

      case Some(correlationId) =>
        request.body.validate[SubscriptionDetails].fold(
          errs =>
            Future.successful(BadRequest(JsError.toJson(errs))),

          details =>
            etmpClient.createSubscription(details, correlationId).map {
              case StcSubscriptionCreateResponse.SuccessResponse(success) => Created(Json.obj("subscriptionId" -> success.stcId))
              case StcSubscriptionCreateResponse.BadRequestResponse(error) => EtmpErrorResponseHelper.badRequestFromError(error)
              case StcSubscriptionCreateResponse.UnprocessableEntityResponse(error) => EtmpErrorResponseHelper.unprocessableEntityFromError(error)
            }.recover {
              case ex =>
                EtmpErrorResponseHelper.logUnexpectedError("createSubscription", ex)
                InternalServerError(Json.obj("message" -> ErrorMessages.UnexpectedError))
            }
        )
    }
  }

  def viewSubscription(subscriptionId: String): Action[AnyContent] = Action.async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      headerValue(request, "correlation-id") match {
        case None => Future.successful(BadRequest(Json.obj("message" -> ErrorMessages.MissingCorrelationId)))

        case Some(correlation) => etmpClient
          .viewSubscription(subscriptionId, correlation)
          .map {
            case StcSubscriptionViewResponse.SuccessResponse(subscription) => Ok(Json.toJson(subscription))
            case StcSubscriptionViewResponse.BadRequestResponse(error) => EtmpErrorResponseHelper.badRequestFromError(error)
            case StcSubscriptionViewResponse.UnprocessableEntityResponse(error) => EtmpErrorResponseHelper.unprocessableEntityFromError(error)
          }.recover {
            case ex =>
              EtmpErrorResponseHelper.logUnexpectedError("viewSubscription", ex)
              InternalServerError(Json.obj("message" -> ErrorMessages.UnexpectedError))
          }
      }
  }

  def amendSubscription(subscriptionId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      headerValue(request, "correlation-id") match {
        case None => Future.successful(BadRequest(Json.obj("message" -> ErrorMessages.MissingCorrelationId)))

        case Some(correlationId) => request.body.validate[Subscription].fold(
          errs =>
            Future.successful(BadRequest(JsError.toJson(errs))),

          subscription => etmpClient
            .amendSubscription(subscriptionId, correlationId, subscription)
            .map {
              case StcSubscriptionAmendResponse.SuccessResponse(success) => Ok(Json.toJson(success))
              case StcSubscriptionAmendResponse.BadRequestResponse(error) => EtmpErrorResponseHelper.badRequestFromError(error)
              case StcSubscriptionAmendResponse.UnprocessableEntityResponse(error) => EtmpErrorResponseHelper.unprocessableEntityFromError(error)
            }.recover {
              case ex =>
                EtmpErrorResponseHelper.logUnexpectedError("amendSubscription", ex)
                InternalServerError(Json.obj("message" -> ErrorMessages.UnexpectedError))
            }
        )
      }
  }
}