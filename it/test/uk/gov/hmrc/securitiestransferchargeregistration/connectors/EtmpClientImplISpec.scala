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
import uk.gov.hmrc.securitiestransferchargeregistration.models.{IndividualRegistrationDetails, IndividualSubscriptionDetails}
import uk.gov.hmrc.securitiestransferchargeregistration.support.WireMockISpecBase

class EtmpClientImplISpec
  extends WireMockISpecBase
    with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val matchingDetails =
    IndividualRegistrationDetails(
      firstName   = "First",
      middleName  = None,
      lastName    = "Last",
      dateOfBirth = "1948-03-13",
      nino        = "AB123456C"
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
      addressLine1 = "1 Test Street",
      addressLine2 = None,
      addressLine3 = None,
      postCode = "AA1 1AA",
      country = "UK",
      telephoneNumber = "01234567890",
      email = "test@test.com"
    )

    "return subscriptionId on 200 OK" in {
      wireMock.stubFor(
        post(urlEqualTo("/securities-transfer-charge-stubs/subscription/individual"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody("""{ "subscriptionId": "SUB123456" }""")
          )
      )

      val cfg = app.injector.instanceOf[AppConfig]
      cfg.stcStubsBaseUrl must include(wireMock.port().toString)

      val client = app.injector.instanceOf[EtmpClient]

      client.subscribeIndividual(details).futureValue mustBe "SUB123456"
    }

    "fail on 400 BAD_REQUEST" in {
      wireMock.stubFor(
        post(urlEqualTo("/securities-transfer-charge-stubs/subscription/individual"))
          .willReturn(
            aResponse()
              .withStatus(400)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"code":"INVALID_PAYLOAD"}""")
          )
      )

      val client = app.injector.instanceOf[EtmpClient]

      val ex = intercept[RuntimeException] {
        client.subscribeIndividual(details).futureValue
      }

      ex.getMessage must include("400")
    }

    "fail on 409 CONFLICT" in {
      wireMock.stubFor(
        post(urlEqualTo("/securities-transfer-charge-stubs/subscription/individual"))
          .willReturn(aResponse().withStatus(409))
      )

      val client = app.injector.instanceOf[EtmpClient]
      val ex = intercept[RuntimeException] {
        client.subscribeIndividual(details).futureValue
      }
      ex.getMessage must include("409")
    }

    "fail on 500 INTERNAL_SERVER_ERROR" in {
      wireMock.stubFor(
        post(urlEqualTo("/securities-transfer-charge-stubs/subscription/individual"))
          .willReturn(aResponse().withStatus(500))
      )

      val client = app.injector.instanceOf[EtmpClient]
      val ex = intercept[RuntimeException] {
        client.subscribeIndividual(details).futureValue
      }
      ex.getMessage must include("500")
    }

    "fail on 503 SERVICE_UNAVAILABLE" in {
      wireMock.stubFor(
        post(urlEqualTo("/securities-transfer-charge-stubs/subscription/individual"))
          .willReturn(aResponse().withStatus(503))
      )

      val client = app.injector.instanceOf[EtmpClient]
      val ex = intercept[RuntimeException] {
        client.subscribeIndividual(details).futureValue
      }
      ex.getMessage must include("503")
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
