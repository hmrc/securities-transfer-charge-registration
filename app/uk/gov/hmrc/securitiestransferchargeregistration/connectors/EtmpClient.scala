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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps}
import uk.gov.hmrc.securitiestransferchargeregistration.config.AppConfig
import uk.gov.hmrc.securitiestransferchargeregistration.models.*
import uk.gov.hmrc.http.HttpReads.Implicits._


import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EtmpClientImpl])
trait EtmpClient {
  def register(details: IndividualRegistrationDetails): Future[String]
  def subscribeIndividual(details: IndividualSubscriptionDetails): Future[String]
  def subscribeOrganisation(details: OrganisationSubscriptionDetails): Future[String]
  def hasCurrentSubscription(etmpSafeId: String): Future[Boolean]
}

@Singleton
final class EtmpClientImpl @Inject()(
                                      http: HttpClientV2,
                                      appConfig: AppConfig
                                    )(implicit ec: ExecutionContext) extends EtmpClient {

  private def registerUrl(nino: String) =
    url"${appConfig.stcStubsBaseUrl}/registration/individual/nino/$nino"

  override def register(details: IndividualRegistrationDetails): Future[String] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val req = EtmpRegistrationRequest(
      regime            = "STC",
      requiresNameMatch = true,
      isAnAgent         = false,
      individual        = EtmpIndividual(
        firstName   = details.firstName,
        lastName    = details.lastName,
        dateOfBirth = Some(details.dateOfBirth)
      )
    )

    http
      .post(registerUrl(details.nino))
      .withBody(Json.toJson(req))
      .execute[EtmpRegistrationSuccessResponse]
      .map(_.safeId)
      .recoverWith {
        case e: HttpException if e.responseCode == Status.BAD_REQUEST =>
          Future.failed(new RuntimeException(s"ETMP register failed: 400 ${e.message}"))
        case e: HttpException if e.responseCode == Status.NOT_FOUND =>
          Future.failed(new RuntimeException("ETMP register failed: NOT_FOUND"))
      }
  }

  private def subscribeIndividualUrl =
    url"${appConfig.stcStubsBaseUrl}/subscription/individual"

  override def subscribeIndividual(details: IndividualSubscriptionDetails): Future[String] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    http
      .post(subscribeIndividualUrl)
      .withBody(Json.toJson(details))
      .execute[EtmpSubscribeSuccessResponse]
      .map(_.subscriptionId)
      .recoverWith {
        case e: HttpException if e.responseCode == Status.BAD_REQUEST =>
          Future.failed(new RuntimeException(s"ETMP subscribeIndividual failed: 400 ${e.message}"))
        case e: HttpException if e.responseCode == Status.NOT_FOUND =>
          Future.failed(new RuntimeException("ETMP subscribeIndividual failed: NOT_FOUND"))
      }
  }

  override def subscribeOrganisation(details: OrganisationSubscriptionDetails): Future[String] =
    Future.failed(new NotImplementedError("subscribeOrganisation not implemented yet"))

  override def hasCurrentSubscription(etmpSafeId: String): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val statusUrl = url"${appConfig.stcStubsBaseUrl}/subscription/$etmpSafeId/status"

    http
      .get(statusUrl)
      .execute[HttpResponse]
      .map { resp =>
        resp.status match {
          case Status.OK        => true
          case Status.NOT_FOUND => false
          case other            => throw new RuntimeException(s"ETMP status unexpected=$other body=${resp.body}")
        }
      }
      .recover {
        case e: HttpException if e.responseCode == Status.NOT_FOUND => false
      }
  }
}

