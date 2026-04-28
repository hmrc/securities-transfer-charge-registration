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
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregistration.connectors.EacdClient
import uk.gov.hmrc.securitiestransferchargeregistration.models.*
import uk.gov.hmrc.securitiestransferchargeregistration.support.ISpecBase

import scala.concurrent.Future

class EnrolmentControllerISpec extends ISpecBase {

  private val enrolIndividualUrl = "/securities-transfer-charge-registration/enrolment/individual"
  private val enrolOrganisationUrl = "/securities-transfer-charge-registration/enrolment/organisation"

  private def eacdStub(enrolSucceeds: Boolean = true): EacdClient =
    new EacdClient {
      override def enrolIndividual(details: IndividualEnrolmentDetails)(implicit hc: HeaderCarrier): Future[Unit] =
        if (enrolSucceeds) Future.successful(())
        else Future.failed(new RuntimeException("enrol failed"))

      override def enrolOrganisation(details: OrganisationEnrolmentDetails)(implicit hc: HeaderCarrier): Future[Unit] =
        if (enrolSucceeds) Future.successful(())
        else Future.failed(new RuntimeException("enrol failed"))
    }

  private def appWith(eacd: EacdClient) =
    appBuilder
      .overrides(
        bind[EacdClient].toInstance(eacd)
      )
      .build()

  "EnrolmentController" should {

    "POST /enrolment/individual - return 204 for valid payload" in {
      val application = appWith(eacdStub())

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
      val application = appWith(eacdStub())

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

    "POST /enrolment/individual - return 500 when enrolment fails" in {
      val application = appWith(eacdStub(enrolSucceeds = false))

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

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      application.stop()
    }

    "POST /enrolment/organisation - return 204 for valid payload" in {
      val application = appWith(eacdStub())

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
      val application = appWith(eacdStub())

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

    "POST /enrolment/organisation - return 500 when enrolment fails" in {
      val application = appWith(eacdStub(enrolSucceeds = false))

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