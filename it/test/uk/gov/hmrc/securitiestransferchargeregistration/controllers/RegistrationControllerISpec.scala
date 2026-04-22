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

import org.scalatest.OptionValues
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregistration.connectors.*
import uk.gov.hmrc.securitiestransferchargeregistration.models.*
import uk.gov.hmrc.securitiestransferchargeregistration.support.ISpecBase

import scala.concurrent.Future

class RegistrationControllerISpec extends ISpecBase with OptionValues {

  private val registerUrl = "/securities-transfer-charge-registration/registration/individual"
  val safeId = "SAFE123"
  val requestJson: JsObject = Json.obj(
    "firstName" -> "Test",
    "middleName" -> "",
    "lastName" -> "Test",
    "dateOfBirth" -> "1990-01-01",
    "nino" -> "AB123456C"
  )

  private def etmpStub(fail: Boolean = false): EtmpClient =
    new EtmpClient {
      override def createSubscription(correlationId: String, subscriptionDetails: SubscriptionDetails)(implicit hc: HeaderCarrier): Future[StcSubscriptionCreateResponse] =
        Future.failed(new NotImplementedError)

      override def viewSubscription(subscriptionId: String, correlationId: String)(implicit hc: HeaderCarrier): Future[StcSubscriptionViewResponse] =
        Future.failed(new NotImplementedError)

      override def amendSubscription(subscriptionId: String, correlationId: String, subscription: Subscription)(implicit hc: HeaderCarrier): Future[StcSubscriptionAmendResponse] =
        Future.failed(new NotImplementedError)

      override def register(details: IndividualRegistrationDetails)(implicit hc: HeaderCarrier): Future[String] =
        if (fail) Future.failed(new RuntimeException("fail")) else Future.successful(safeId)
    }


  private def appWith(etmp: EtmpClient) =
    appBuilder
      .overrides(
        bind[EtmpClient].toInstance(etmp),
      )
      .build()

  "RegistrationController" should {

    "POST /registration/individual - return 200 and JSON safeId for valid payload" in {
      val application = appWith(etmpStub())

      running(application) {


        val request =
          FakeRequest(POST, registerUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(requestJson)

        val result = route(application, request).value
        status(result) mustBe OK
        (contentAsJson(result) \ "safeId").as[String] mustBe safeId
      }

      application.stop()
    }

    "POST /registration/individual - return 400 for invalid payload" in {
      val application = appWith(etmpStub())

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

    "POST /registration/individual - return 500 on error " in {
      val application = appWith(etmpStub(fail = true))

      running(application) {
        val request =
          FakeRequest(POST, registerUrl)
            .withHeaders("Content-Type" -> "application/json")
            .withBody(requestJson)

        val result = route(application, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
      application.stop()
    }
  }

}