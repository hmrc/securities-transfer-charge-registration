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
import uk.gov.hmrc.securitiestransferchargeregistration.models.{IndividualRegistrationDetails, IndividualSubscriptionDetails, OrganisationSubscriptionDetails}
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
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody(responseBody)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]
      client.subscribeIndividual(details).futureValue mustBe "XASTS0123456789"
    }

    "fail with SubscriptionResponseParseError when parsing the JSON response fails" in {
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
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody(invalidResponseBody)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      val result = client.subscribeIndividual(details)
      whenReady(result.failed) { ex =>
        ex mustBe a[SubscriptionResponseParseError]
        ex.getMessage must include("Failed to parse JSON response")
      }
    }

    "fail with SubscriptionErrorException for non-201 responses" in {
      val testCases = Seq(
        400 -> "Bad Request",
        401 -> "Unauthorized",
        403 -> "Forbidden",
        404 -> "Not Found",
        500 -> "Internal Server Error"
      )

      testCases.foreach { case (status, _) =>
        val body = s"""{"error":"status $status"}"""
        wireMock.stubFor(
          post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
            .willReturn(
              aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)
            )
        )

        val client = app.injector.instanceOf[EtmpClient]
        val result = client.subscribeIndividual(details)

        whenReady(result.failed) { ex =>
          ex mustBe a[SubscriptionErrorException]
          ex.getMessage must include(status.toString)
        }
      }
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
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody(responseBody)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]
      client.subscribeOrganisation(details).futureValue mustBe "XASTS0123456789"
    }

    "fail with SubscriptionResponseParseError when parsing the JSON response fails" in {
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
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody(invalidResponseBody)
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      val result = client.subscribeOrganisation(details)
      whenReady(result.failed) { ex =>
        ex mustBe a[SubscriptionResponseParseError]
        ex.getMessage must include("Failed to parse JSON response")
      }
    }

    "fail with SubscriptionErrorException for non-201 responses" in {
      val testCases = Seq(
        400 -> "Bad Request",
        401 -> "Unauthorized",
        403 -> "Forbidden",
        404 -> "Not Found",
        500 -> "Internal Server Error"
      )

      testCases.foreach { case (status, _) =>
        val body = s"""{"error":"status $status"}"""
        wireMock.stubFor(
          post(urlEqualTo(s"/securities-transfer-charge-stubs/stc/subscription/${details.safeId}"))
            .willReturn(
              aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)
            )
        )

        val client = app.injector.instanceOf[EtmpClient]
        val result = client.subscribeOrganisation(details)

        whenReady(result.failed) { ex =>
          ex mustBe a[SubscriptionErrorException]
          ex.getMessage must include(status.toString)
        }
      }
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
