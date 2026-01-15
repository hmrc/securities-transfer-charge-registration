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

import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargeregistration.connectors.{EacdClient, EtmpClient}
import uk.gov.hmrc.securitiestransferchargeregistration.models.IndividualRegistrationDetails
import uk.gov.hmrc.securitiestransferchargeregistration.support.ISpecBase
import uk.gov.hmrc.securitiestransferchargeregistration.models.*
import org.scalatest.OptionValues

import scala.concurrent.Future

class RegistrationControllerISpec extends ISpecBase with OptionValues {

  private val registerUrl             = "/securities-transfer-charge-registration/registration/individual"
  private val subscribeIndividualUrl  = "/securities-transfer-charge-registration/subscription/individual"
  private val subscribeOrganisationUrl  = "/securities-transfer-charge-registration/subscription/organisation"
  private val enrolIndividualUrl      = "/securities-transfer-charge-registration/enrolment/individual"
  private val enrolOrganisationUrl      = "/securities-transfer-charge-registration/enrolment/organisation"

  private def subscriptionStatusUrl(safeId: String) =
    s"/securities-transfer-charge-registration/subscription/$safeId/status"

  private def etmpStub(
                        safeId: String = "SAFE123",
                        subscriptionId: String = "SUB123",
                        statusResult: Boolean = true
                      ): EtmpClient =
    new EtmpClient {

      override def register(details: IndividualRegistrationDetails): Future[String] =
        Future.successful(safeId)

      override def subscribeIndividual(details: IndividualSubscriptionDetails): Future[String] =
        Future.successful(subscriptionId)

      override def subscribeOrganisation(details: OrganisationSubscriptionDetails): Future[String] =
        Future.successful(subscriptionId)

      override def hasCurrentSubscription(etmpSafeId: String): Future[Boolean] =
        Future.successful(statusResult)
    }

  private def eacdStub(enrolSucceeds: Boolean = true): EacdClient =
    new EacdClient {
      override def enrolIndividual(details: IndividualEnrolmentDetails): Future[Unit] =
        if (enrolSucceeds) Future.successful(())
        else Future.failed(new RuntimeException("enrol failed"))

      override def enrolOrganisation(details: OrganisationEnrolmentDetails): Future[Unit] =
        if (enrolSucceeds) Future.successful(())
        else Future.failed(new RuntimeException("enrol failed"))
    }

  private def appWith(etmp: EtmpClient, eacd: EacdClient) =
    appBuilder
      .overrides(
        bind[EtmpClient].toInstance(etmp),
        bind[EacdClient].toInstance(eacd)
      )
      .build()

  "RegistrationController" should {

    "POST /registration/individual - return 200 and JSON safeId for valid payload" in {
      val application = appWith(etmpStub(safeId = "SAFE123"), eacdStub())

      running(application) {
        val requestJson = Json.obj(
          "firstName" -> "Test",
          "middleName" -> "",
          "lastName" -> "Test",
          "dateOfBirth" -> "1990-01-01",
          "nino" -> "AB123456C"
        )

        val request =
          FakeRequest(POST, registerUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(requestJson)

        val result = route(application, request).value
        status(result) mustBe OK
        (contentAsJson(result) \ "safeId").as[String] mustBe "SAFE123"
      }

      application.stop()
    }

    "POST /registration/individual - return 400 for invalid payload" in {
      val application = appWith(etmpStub(), eacdStub())

      running(application) {
        val badJson = Json.obj("bad" -> "json")

        val request =
          FakeRequest(POST, registerUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(badJson)

        val result = route(application, request).value
        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "POST /subscription/individual - return 200 and JSON subscriptionId for valid payload" in {
      val application = appWith(etmpStub(subscriptionId = "SUB123"), eacdStub())

      running(application) {
        val requestJson = Json.obj(
          "safeId" -> "SAFE123",
          "addressLine1" -> "1 Test Street",
          "addressLine2" -> "",
          "addressLine3" -> "",
          "postCode" -> "AA1 1AA",
          "country" -> "UK",
          "telephoneNumber" -> "01234567890",
          "email" -> "test@test.com"
        )

        val request =
          FakeRequest(POST, subscribeIndividualUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(requestJson)

        val result = route(application, request).value
        status(result) mustBe OK
        (contentAsJson(result) \ "subscriptionId").as[String] mustBe "SUB123"
      }

      application.stop()
    }

    "POST /subscription/individual - return 400 for invalid payload" in {
      val application = appWith(etmpStub(), eacdStub())

      running(application) {
        val badJson = Json.obj("bad" -> "json")

        val request =
          FakeRequest(POST, subscribeIndividualUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(badJson)

        val result = route(application, request).value
        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "POST /enrolment/individual - return 204 for valid payload" in {
      val application = appWith(etmpStub(), eacdStub(enrolSucceeds = true))

      running(application) {
        val requestJson = Json.obj(
          "subscriptionId" -> "SUB123",
          "nino" -> "AB123456C"
        )

        val request =
          FakeRequest(POST, enrolIndividualUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(requestJson)

        val result = route(application, request).value
        status(result) mustBe NO_CONTENT
      }

      application.stop()
    }

    "POST /enrolment/individual - return 400 for invalid payload" in {
      val application = appWith(etmpStub(), eacdStub())

      running(application) {
        val badJson = Json.obj("bad" -> "json")

        val request =
          FakeRequest(POST, enrolIndividualUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(badJson)

        val result = route(application, request).value
        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "GET /subscription/:safeId/status - return 200 when active" in {
      val application = appWith(etmpStub(), eacdStub())

      running(application) {
        val request =
          FakeRequest(GET, subscriptionStatusUrl("SAFE123"))

        val result = route(application, request).value
        status(result) mustBe OK
      }

      application.stop()
    }

    "GET /subscription/:safeId/status - return 404 when not found" in {
      val application = appWith(etmpStub(statusResult = false), eacdStub())

      running(application) {
        val request =
          FakeRequest(GET, subscriptionStatusUrl("SAFE123"))

        val result = route(application, request).value
        status(result) mustBe NOT_FOUND
      }

      application.stop()
    }

    "POST /subscription/organisation - return 200 and JSON subscriptionId for valid payload" in {
      val application = appWith(etmpStub(), eacdStub())

      running(application) {
        val orgSubscriptionRequestJson = Json.obj(
          "safeId" -> "SAFE123",
          "addressLine1" -> "1 Test Street",
          "addressLine2" -> "",
          "addressLine3" -> "",
          "postCode" -> "AA1 1AA",
          "country" -> "UK",
          "telephoneNumber" -> "01234567890",
          "email" -> "test@test.com"
        )

        val request =
          FakeRequest(POST, subscribeOrganisationUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(orgSubscriptionRequestJson)

        val result = route(application, request).value
        status(result) mustBe OK
        (contentAsJson(result) \ "subscriptionId").as[String] mustBe "SUB123"
      }

      application.stop()
    }

    "POST /subscription/organisation - return 400 for invalid payload" in {
      val application = appWith(etmpStub(), eacdStub())

      running(application) {
        val badJson = Json.obj("bad" -> "json")

        val request =
          FakeRequest(POST, subscribeOrganisationUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(badJson)

        val result = route(application, request).value
        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "POST /enrolment/organisation - return 204 for valid payload" in {
      val application = appWith(etmpStub(), eacdStub())

      running(application) {
        val requestJson = Json.obj(
          "subscriptionId" -> "SUB123",
          "ctUtr" -> "0123456789"
        )

        val request =
          FakeRequest(POST, enrolOrganisationUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(requestJson)

        val result = route(application, request).value
        status(result) mustBe NO_CONTENT
      }

      application.stop()
    }

    "POST /enrolment/organisation - return 400 for invalid payload" in {
      val application = appWith(etmpStub(), eacdStub())

      running(application) {
        val badJson = Json.obj("bad" -> "json")

        val request =
          FakeRequest(POST, enrolOrganisationUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(badJson)

        val result = route(application, request).value
        status(result) mustBe BAD_REQUEST
      }

      application.stop()
    }

    "POST /enrolment/organisation - return 500 internal Server Error when enrolment fails" in {
      val application = appWith(etmpStub(), eacdStub(enrolSucceeds = false))

      running(application) {
        val requestJson = Json.obj(
          "subscriptionId" -> "SUB123",
          "ctUtr" -> "0123456789"
        )

        val request =
          FakeRequest(POST, enrolOrganisationUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(requestJson)

        val result = route(application, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      application.stop()
    }
  }
}