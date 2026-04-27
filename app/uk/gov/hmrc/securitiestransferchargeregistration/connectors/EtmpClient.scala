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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.securitiestransferchargeregistration.config.AppConfig
import uk.gov.hmrc.securitiestransferchargeregistration.models.*

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EtmpClientImpl])
trait EtmpClient {
  def register(details: IndividualRegistrationDetails)(implicit hc: HeaderCarrier): Future[String]
  
  def viewSubscription(subscriptionId: String, correlationId: String)(implicit hc: HeaderCarrier): Future[StcSubscriptionViewResponse]

  def amendSubscription(subscriptionId: String, correlationId: String, subscription: Subscription)(implicit hc: HeaderCarrier): Future[StcSubscriptionAmendResponse]

  def createSubscription(subscriptionDetails: SubscriptionDetails,correlationId: String)(implicit hc: HeaderCarrier): Future[StcSubscriptionCreateResponse]
}

@Singleton
final class EtmpClientImpl @Inject()(
                                      http: HttpClientV2,
                                      appConfig: AppConfig,
                                      clock: Clock,
                                    )(implicit ec: ExecutionContext) extends EtmpClient {
  private val dateTimeFormatter = DateTimeFormatter.ISO_INSTANT

  private def headers(correlationId: String, receiptDate: String)=
    Seq(
      "correlationid" -> correlationId,
      "X-Originating-System" -> appConfig.etmpOriginatingSystem,
      "X-Receipt-Date" -> receiptDate,
      "X-Transmitting-System" -> appConfig.etmpTransmittingSystem
    )


  private def registerUrl(nino: String) =
    url"${appConfig.stcStubsBaseUrl}/registration/individual/nino/$nino"

  override def register(details: IndividualRegistrationDetails)(implicit hc: HeaderCarrier): Future[String] = {

    val req = EtmpRegistrationRequest(
      regime = "STC",
      requiresNameMatch = true,
      isAnAgent = false,
      individual = EtmpIndividual(
        firstName = details.firstName,
        lastName = details.lastName,
        dateOfBirth = Some(details.dateOfBirth)
      )
    )

    http
      .post(registerUrl(details.nino))
      .withBody(Json.toJson(req))
      .execute[EtmpRegistrationSuccessResponse]
      .map(_.safeId)
      .recoverWith {
        case e: UpstreamErrorResponse =>
          Future.failed(e.statusCode match {
            case 400 => EtmpBadRequest
            case 404 => EtmpNotFound
            case 409 => EtmpConflict
            case 500 => EtmpServerError
            case 503 => EtmpServiceUnavailable
            case s => EtmpUnexpected(s)
          })
      }
  }
  
  override def viewSubscription(subscriptionId: String, correlationId: String)(implicit hc: HeaderCarrier): Future[StcSubscriptionViewResponse] = {
    val receiptDate = dateTimeFormatter.format(Instant.now(clock))

    http
      .get(url"${appConfig.stcStubsBaseUrl}/RESTAdapter/stc/subscription/$subscriptionId")
      .setHeader(headers(correlationId, receiptDate): _*)
      .execute[HttpResponse]
      .map(StcSubscriptionViewResponse.fromHttpResponse)
  }

  override def amendSubscription(subscriptionId: String, correlationId: String, subscription: Subscription)(implicit hc: HeaderCarrier): Future[StcSubscriptionAmendResponse] = {
    val receiptDate = dateTimeFormatter.format(Instant.now(clock))

    http
      .put(url"${appConfig.stcStubsBaseUrl}/RESTAdapter/stc/subscription/$subscriptionId")
      .setHeader(headers(correlationId, receiptDate): _*)
      .withBody(Json.toJson(subscription))
      .execute[HttpResponse]
      .map(StcSubscriptionAmendResponse.fromHttpResponse)
  }

  override def createSubscription(
                                   subscriptionDetails: SubscriptionDetails,
                                   correlationId: String
                                 )(implicit hc: HeaderCarrier): Future[StcSubscriptionCreateResponse] = {

    val receiptDate = dateTimeFormatter.format(Instant.now(clock))

    http
      .post(url"${appConfig.stcStubsBaseUrl}/RESTAdapter/stc/subscription/${subscriptionDetails.safeId}")
      .setHeader(headers(correlationId, receiptDate): _*)
      .withBody(SubscriptionDetails.toJson(subscriptionDetails))
      .execute[HttpResponse]
      .map(StcSubscriptionCreateResponse.fromHttpResponse)
  }
}