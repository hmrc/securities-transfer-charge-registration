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

package uk.gov.hmrc.securitiestransferchargeregistration.controllers

import org.scalatest.OptionValues.convertOptionToValuable
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregistration.connectors.{IndividualSubscriptionDetails,OrganisationSubscriptionDetails, *}
import uk.gov.hmrc.securitiestransferchargeregistration.connectors.StcSubscriptionCreateResponse.SubscriptionSuccess
import uk.gov.hmrc.securitiestransferchargeregistration.models.*
import uk.gov.hmrc.securitiestransferchargeregistration.support.ISpecBase

import java.time.LocalDate
import scala.concurrent.Future

class SubscriptionControllerISpec extends ISpecBase {

  private val subscribeIndividualUrl   = "/securities-transfer-charge-registration/subscription/individual"
  private val subscribeOrganisationUrl = "/securities-transfer-charge-registration/subscription/organisation"
  private val viewSubscriptionUrl      = (id: String) =>
    s"/securities-transfer-charge-registration/subscription/$id"
  private val amendSubscriptionUrl     = (id: String) =>
    s"/securities-transfer-charge-registration/subscription/$id"

  private val correlationId = "corr-id-123"

  val subscriptionDetails: IndividualSubscriptionDetails = IndividualSubscriptionDetails(
    safeId = "XAS1234567890",
    contactName = "John Mill",
    addressLine1 = "350 But Close",
    addressLine2 = None,
    addressLine3 = None,
    postcode = "SE10 7KGT",
    countryCode = "GB",
    telephoneNumber = "0777777777",
    email = "some@email.com"
  )

  val stcId = "STC12345"
  val subscriptionId = "XA123445"
  val date: String = LocalDate.now().toString
  val requestJson: JsValue = Json.toJson(subscriptionDetails)

  private def etmpStub(
                        createResponse: StcSubscriptionCreateResponse = StcSubscriptionCreateResponse.SuccessResponse(SubscriptionSuccess(processingDate = date, stcId = stcId))
                        ,
                        viewResponse: StcSubscriptionViewResponse =
                        StcSubscriptionViewResponse.SuccessResponse(StcSubscriptionViewResponse.SubscriptionDetails(
                          processingDate = date,
                          subsValidTo = date,
                          contactName = subscriptionDetails.contactName,
                          addressLine1 = subscriptionDetails.addressLine1,
                          addressLine2 = None,
                          addressLine3 = None,
                          postcode = subscriptionDetails.postcode,
                          countryCode = subscriptionDetails.countryCode,
                          telephoneNumber = subscriptionDetails.telephoneNumber,
                          mobileNumber = None,
                          email = subscriptionDetails.email
                        )),
                        amendResponse: StcSubscriptionAmendResponse =
                        StcSubscriptionAmendResponse.SuccessResponse(StcSubscriptionAmendResponse.SuccessDetails(processingDate = date))
                        ,
                        fail: Boolean = false
                      ): EtmpClient =
    new EtmpClient {

      override def createSubscription(correlationId: String, details: SubscriptionDetails)(implicit hc: HeaderCarrier): Future[StcSubscriptionCreateResponse] =
        if (fail) Future.failed(new RuntimeException("fail"))
        else Future.successful(createResponse)

      override def viewSubscription(subscriptionId: String, correlationId: String)(implicit hc: HeaderCarrier): Future[StcSubscriptionViewResponse] =
        if (fail) Future.failed(new RuntimeException("fail"))
        else Future.successful(viewResponse)

      override def amendSubscription(subscriptionId: String, correlationId: String, subscription: Subscription)(implicit hc: HeaderCarrier): Future[StcSubscriptionAmendResponse] =
        if (fail) Future.failed(new RuntimeException("fail"))
        else Future.successful(amendResponse)

      override def register(details: IndividualRegistrationDetails)(implicit hc: HeaderCarrier): Future[String] =
        Future.successful("SAFE123")

    }

  private def appWith(etmp: EtmpClient) =
    appBuilder
      .overrides(
        bind[EtmpClient].toInstance(etmp)
      )
      .build()

  "SubscriptionController" should {

    "POST /subscription/individual - return 201 for valid payload" in {
      val application = appWith(etmpStub())

      running(application) {

        val request =
          FakeRequest(POST, subscribeIndividualUrl)
            .withHeaders(
              "Content-Type" -> "application/json",
              "correlation-id" -> correlationId
            )
            .withBody(requestJson)

        val result = route(application, request).value

        status(result) mustBe CREATED
      }

      application.stop()
    }

    "POST /subscription/individual - return 400 when correlation id missing" in {
      val application = appWith(etmpStub())

      running(application) {
        val request =
          FakeRequest(POST, subscribeIndividualUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(Json.obj())

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "POST /subscription/individual - return 400 for invalid payload" in {
      val application = appWith(etmpStub())

      running(application) {
        val request =
          FakeRequest(POST, subscribeIndividualUrl)
            .withHeaders(
              "Content-Type" -> "application/json",
              "correlation-id" -> correlationId
            )
            .withBody(Json.obj("bad" -> "json"))

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "POST /subscription/individual - return 500 on unexpected error" in {
      val application = appWith(etmpStub(fail = true))

      val requestJson = Json.toJson(subscriptionDetails)

      running(application) {
        val request =
          FakeRequest(POST, subscribeIndividualUrl)
            .withHeaders(
              "Content-Type" -> "application/json",
              "correlation-id" -> correlationId
            )
            .withBody(requestJson)

        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      application.stop()
    }

    "POST /subscription/organisation - return 201 for valid payload" in {

      val organisationSubscriptionDetails = OrganisationSubscriptionDetails(
        safeId = "XAS1234567890",
        contactName="Some name",
        addressLine1 = "350 But Close",
        addressLine2 = None,
        addressLine3 = None,
        postcode = "SE10 7KGT",
        countryCode = "GB",
        telephoneNumber = "0777777777",
        email = "some@email.com"
      )
      val application = appWith(etmpStub())

      running(application) {

        val request =
          FakeRequest(POST, subscribeOrganisationUrl)
            .withHeaders(
              "Content-Type" -> "application/json",
              "correlation-id" -> correlationId
            )
            .withBody(Json.toJson(organisationSubscriptionDetails))

        val result = route(application, request).value

        status(result) mustBe CREATED
      }

      application.stop()
    }

    "GET /subscription/:subscriptionId - return 200 when found" in {
      val application = appWith(etmpStub())

      running(application) {
        val request =
          FakeRequest(GET, viewSubscriptionUrl(subscriptionId))
            .withHeaders("correlation-id" -> correlationId)

        val result = route(application, request).value

        status(result) mustBe OK
      }

      application.stop()
    }

    "GET /subscription/:subscriptionId - return 400 when correlation id missing" in {
      val application = appWith(etmpStub())

      running(application) {
        val request =
          FakeRequest(GET, viewSubscriptionUrl(subscriptionId))

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "GET /subscription/:subscriptionId - return 500 on error" in {
      val application = appWith(etmpStub(fail = true))

      running(application) {
        val request =
          FakeRequest(GET, viewSubscriptionUrl(subscriptionId))
            .withHeaders("correlation-id" -> correlationId)

        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      application.stop()
    }

    "PUT /subscription/:subscriptionId - return 200 for valid amend" in {
      val application = appWith(etmpStub())
      val updated = subscriptionDetails.copy(contactName = "Updated name",addressLine1 = "Updated Address")

      running(application) {
        val request =
          FakeRequest(PUT, amendSubscriptionUrl(subscriptionId))
            .withHeaders(
              "Content-Type" -> "application/json",
              "correlation-id" -> correlationId
            )
            .withBody(Json.toJson(updated))

        val result = route(application, request).value

        status(result) mustBe OK
      }

      application.stop()
    }

    "PUT /subscription/:id - return 400 for invalid payload" in {
      val application = appWith(etmpStub())

      running(application) {
        val request =
          FakeRequest(PUT, amendSubscriptionUrl(subscriptionId))
            .withHeaders(
              "Content-Type" -> "application/json",
              "correlation-id" -> correlationId
            )
            .withBody(Json.obj("bad" -> "json"))

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "PUT /subscription/:id - return 400 when correlation id missing" in {
      val application = appWith(etmpStub())

      running(application) {
        val request =
          FakeRequest(PUT, amendSubscriptionUrl(subscriptionId))
            .withHeaders("Content-Type" -> "application/json")
            .withBody(Json.obj())

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "PUT /subscription/:id - return 500 on error" in {
      val application = appWith(etmpStub(fail = true))

      running(application) {
        val request =
          FakeRequest(PUT, amendSubscriptionUrl(subscriptionId))
            .withHeaders(
              "Content-Type" -> "application/json",
              "correlation-id" -> correlationId
            )
            .withBody(requestJson)

        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      application.stop()
    }
  }
}