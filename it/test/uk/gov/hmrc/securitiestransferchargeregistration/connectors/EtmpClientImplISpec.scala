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

package uk.gov.hmrc.securitiestransferchargeregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregistration.config.AppConfig
import uk.gov.hmrc.securitiestransferchargeregistration.models.SubscriptionFailure.UnexpectedStatus
import uk.gov.hmrc.securitiestransferchargeregistration.models.{IndividualRegistrationDetails, IndividualSubscriptionDetails, OrganisationSubscriptionDetails, SubscriptionFailure}
import uk.gov.hmrc.securitiestransferchargeregistration.support.WireMockISpecBase

class EtmpClientImplISpec
  extends WireMockISpecBase
    with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val matchingDetails =
    IndividualRegistrationDetails(
      firstName = "First",
      middleName = None,
      lastName = "Last",
      dateOfBirth = "1948-03-13",
      nino = "AB123456C"
    )

  private def stubRegister(nino: String, status: Int, body: String): Unit =
    wireMock.stubFor(
      post(urlEqualTo(s"/securities-transfer-charge-stubs/registration/individual/nino/$nino"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(body)
        )
    )

  "EtmpClient.register" should {

    "return safeId on 200 OK" in {
      stubRegister("AB123456C", 200, """{ "safeId": "XE0001234567890" }""")

      val client = app.injector.instanceOf[EtmpClient]

      client.register(matchingDetails).futureValue mustBe "XE0001234567890"
    }

    "fail on 404 NOT_FOUND" in {
      stubRegister("AA123456A", 404, """{ "code":"NOT_FOUND", "reason":"no data" }""")

      val client = app.injector.instanceOf[EtmpClient]

      val ex = intercept[RuntimeException] {
        client.register(matchingDetails.copy(nino = "AA123456A")).futureValue
      }

      ex.getMessage must include("NOT_FOUND")
    }
  }

  "EtmpClient.subscribeOrganisation" should {

    val details = OrganisationSubscriptionDetails(
      safeId = "XE0001234567890",
      addressLine1 = "1 Test Street",
      addressLine2 = None,
      addressLine3 = None,
      postCode = "AA1 1AA",
      country = "UK",
      telephoneNumber = "01234567890",
      email = "test@test.com"
    )

    "successfully return a subscriptionId" in {
      val responseBody =
        """{
          |  "success": {
          |    "processingDate": "2025-01-10T09:30:47Z",
          |    "stcId": "XASTS0123456789"
          |  }
          |}""".stripMargin
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(responseBody)
          )
      )

      val cfg = app.injector.instanceOf[AppConfig]
      cfg.stcStubsBaseUrl must include(wireMock.port().toString)

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeOrganisation(details).futureValue mustBe Right("XASTS0123456789")
    }

    "return Left(UnexpectedStatus) when ETMP returns a 500" in {
      val responsePayload =
        """{
          |  "error": {
          |    "code": "500",
          |    "message": "string",
          |    "logID": "5A25056E296423F68695903376F6E59A"
          |  }
          |}""".stripMargin
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withHeader("Content-Type", "application/json")
              .withBody(responsePayload)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeOrganisation(details).futureValue mustBe Left(UnexpectedStatus(500, responsePayload))
    }

    "return Left(SubscriptionFailure.NotFound) when ETMP returns a 404" in {
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(404)
              .withHeader("Content-Type", "application/json")
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeOrganisation(details).futureValue mustBe Left(SubscriptionFailure.NotFound)
    }

    "return Left(SubscriptionFailure.Forbidden) when ETMP returns a 403" in {
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(403)
              .withHeader("Content-Type", "application/json")
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeOrganisation(details).futureValue mustBe Left(SubscriptionFailure.Forbidden)
    }

    "return  Left(SubscriptionFailure.Unauthorized) when ETMP returns a 401" in {
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(401)
              .withHeader("Content-Type", "application/json")
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeOrganisation(details).futureValue mustBe Left(SubscriptionFailure.Unauthorized)
    }

    "return left(SubscriptionFailure.InvalidErrorResponse when ETMP returns a 400" in {
      val responsePayload =
        """{
          |  "error": {
          |    "code": "400",
          |    "message": "string",
          |    "logID": "CFBE36D42271A9168125B7B5E6EB54B1"
          |  }
          |}""".stripMargin
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(400)
              .withHeader("Content-Type", "application/json")
              .withBody(responsePayload)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeOrganisation(details).futureValue mustBe Left(SubscriptionFailure.InvalidErrorResponse(400, responsePayload))
    }


    "return Left(SubscriptionFailure.InvalidSuccessResponse) when parsing the json response fails" in {
      val invalidResponseBody =
        """{
          |  "success": {
          |    "processingDate": "2025-01-10T09:30:47Z"
          |  }
          |}""".stripMargin
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(invalidResponseBody)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeOrganisation(details).futureValue mustBe Left(SubscriptionFailure.InvalidSuccessResponse(invalidResponseBody))
    }


  }

  "EtmpClient.subscribeIndividual" should {

    val details = IndividualSubscriptionDetails(
      safeId = "XE0001234567890",
      contactName = "Test Name",
      addressLine1 = "1 Test Street",
      addressLine2 = None,
      addressLine3 = None,
      postCode = "AA1 1AA",
      country = "UK",
      telephoneNumber = "01234567890",
      email = "test@test.com"
    )

    "successfully return a subscriptionId" in {
      val responseBody =
        """{
          |  "success": {
          |    "processingDate": "2025-01-10T09:30:47Z",
          |    "stcId": "XASTS0123456789"
          |  }
          |}""".stripMargin
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(responseBody)
          )
      )

      val cfg = app.injector.instanceOf[AppConfig]
      cfg.stcStubsBaseUrl must include(wireMock.port().toString)

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeIndividual(details).futureValue mustBe Right("XASTS0123456789")
    }

    "return left(SubscriptionFailure.InvalidErrorResponse when ETMP returns a 400" in {
      val responsePayload =
        """{
          |  "error": {
          |    "code": "400",
          |    "message": "string",
          |    "logID": "CFBE36D42271A9168125B7B5E6EB54B1"
          |  }
          |}""".stripMargin
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(400)
              .withHeader("Content-Type", "application/json")
              .withBody(responsePayload)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeIndividual(details).futureValue mustBe Left(SubscriptionFailure.InvalidErrorResponse(400, responsePayload))
    }

    "return Left(SubscriptionFailure.NotFound) when ETMP returns a 404" in {
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(404)
              .withHeader("Content-Type", "application/json")
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeIndividual(details).futureValue mustBe Left(SubscriptionFailure.NotFound)
    }

    "return Left(SubscriptionFailure.Forbidden) when ETMP returns a 403" in {
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(403)
              .withHeader("Content-Type", "application/json")
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeIndividual(details).futureValue mustBe Left(SubscriptionFailure.Forbidden)
    }

    "return  Left(SubscriptionFailure.Unauthorized) when ETMP returns a 401" in {
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(401)
              .withHeader("Content-Type", "application/json")
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeIndividual(details).futureValue mustBe Left(SubscriptionFailure.Unauthorized)
    }
    "return Left(SubscriptionFailure.InvalidSuccessResponse) when parsing the json response fails" in {
      val invalidResponseBody =
        """{
          |  "success": {
          |    "processingDate": "2025-01-10T09:30:47Z"
          |  }
          |}""".stripMargin
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(invalidResponseBody)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeIndividual(details).futureValue mustBe Left(SubscriptionFailure.InvalidSuccessResponse(invalidResponseBody))
    }

    "return Left(UnexpectedStatus) when ETMP returns a 500" in {
      val responsePayload =
        """{
          |  "error": {
          |    "code": "500",
          |    "message": "string",
          |    "logID": "5A25056E296423F68695903376F6E59A"
          |  }
          |}""".stripMargin
      wireMock.stubFor(
        post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withHeader("Content-Type", "application/json")
              .withBody(responsePayload)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeIndividual(details).futureValue mustBe Left(UnexpectedStatus(500, responsePayload))
    }

  }

  "EtmpClient.hasCurrentSubscription" should {

    "return true on 200 OK" in {
      wireMock.stubFor(
        get(urlEqualTo("/securities-transfer-charge-stubs/subscription/SAFE123/status"))
          .willReturn(aResponse().withStatus(200))
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.hasCurrentSubscription("SAFE123").futureValue mustBe true
    }

    "return false on 404 NOT_FOUND" in {
      wireMock.stubFor(
        get(urlEqualTo("/securities-transfer-charge-stubs/subscription/SAFE404/status"))
          .willReturn(aResponse().withStatus(404))
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.hasCurrentSubscription("SAFE404").futureValue mustBe false
    }

    "fail on 500 INTERNAL_SERVER_ERROR" in {
      wireMock.stubFor(
        get(urlEqualTo("/securities-transfer-charge-stubs/subscription/SAFE500/status"))
          .willReturn(aResponse().withStatus(500))
      )

      val client = app.injector.instanceOf[EtmpClient]

      intercept[Exception] {
        client.hasCurrentSubscription("SAFE500").futureValue
      }
    }
  }
}
