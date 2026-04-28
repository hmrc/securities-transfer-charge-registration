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
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregistration.models.*
import uk.gov.hmrc.securitiestransferchargeregistration.support.WireMockISpecBase

import java.time.{Clock, Instant, ZoneOffset}


class EtmpClientImplISpec
  extends WireMockISpecBase
    with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val correlationId = "correlation1234"
  val subscriptionId = "XA12345"

  private val fixedClock: Clock =
    Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC)

  override lazy val appBuilder: GuiceApplicationBuilder =
    super.appBuilder
      .overrides(
        inject.bind[Clock].toInstance(fixedClock)
      )

  private val baseUrl = "/securities-transfer-charge-stubs"

  private def stubPost(url: String, status: Int, body: String): Unit =
    wireMock.stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(body)
        )
    )

  private def stubGet(url: String, status: Int, body: String = ""): Unit =
    wireMock.stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(body)
        )
    )

  private def stubPut(url: String, status: Int, body: String): Unit =
    wireMock.stubFor(
      put(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(body)
        )
    )

  "EtmpClient.register" should {

    val details = IndividualRegistrationDetails(
      firstName = "First",
      middleName = None,
      lastName = "Last",
      dateOfBirth = "1990-01-01",
      nino = "AB123456C"
    )

    "return safeId on 200 OK" in {
      stubPost(
        s"$baseUrl/registration/individual/nino/${details.nino}",
        200,
        """{ "safeId": "SAFE123" }"""
      )

      val client = app.injector.instanceOf[EtmpClient]

      whenReady(client.register(details), Timeout(Span(2, Seconds))) { result =>
        result mustBe "SAFE123"
      }
    }

    "map 400 to EtmpBadRequest" in {
      stubPost(
        s"$baseUrl/registration/individual/nino/${details.nino}",
        400,
        """{ "code": "BAD_REQUEST", "reason": "invalid" }"""
      )

      val client = app.injector.instanceOf[EtmpClient]

      val ex = client.register(details).failed.futureValue

      ex mustBe EtmpBadRequest
    }


    "map 404 to EtmpNotFound" in {
      stubPost(
        s"$baseUrl/registration/individual/nino/${details.nino}",
        404,
        """{}"""
      )

      val client = app.injector.instanceOf[EtmpClient]

      val ex = client.register(details).failed.futureValue

      ex mustBe EtmpNotFound
    }

    "map 500 to EtmpServerError" in {
      stubPost(
        s"$baseUrl/registration/individual/nino/${details.nino}",
        500,
        """{}"""
      )

      val client = app.injector.instanceOf[EtmpClient]

      val ex = client.register(details).failed.futureValue

      ex mustBe EtmpServerError
    }
  }

  "EtmpClient.createSubscription" should {

    val details = SubscriptionDetails(
      safeId = "SAFE123",
      contactName = "Test",
      addressLine1 = "1 Street",
      addressLine2 = None,
      addressLine3 = None,
      postcode = "AA1 1AA",
      countryCode = "UK",
      telephoneNumber = "0123456789",
      email = "test@test.com"
    )

    "return SuccessResponse on 201" in {
      val response =
        """{
          | "success": {
          |   "processingDate": "2025-01-01T00:00:00Z",
          |   "stcId": "STC123"
          | }
          |}""".stripMargin

      stubPost(
        s"$baseUrl/RESTAdapter/stc/subscription/${details.safeId}",
        201,
        response
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.createSubscription(details,correlationId).futureValue mustBe
        a[StcSubscriptionCreateResponse.SuccessResponse]
    }

    "return BadRequestResponse on 400" in {
      val body = """{
                   |  "error": {
                   |    "code": "400",
                   |    "message": "string",
                   |    "logID": "2F86531D62BD77DC215DFC8B97A34F32"
                   |  }
                   |}""".stripMargin
      stubPost(
        s"$baseUrl/RESTAdapter/stc/subscription/${details.safeId}",
        400,
        body
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.createSubscription(details,correlationId).futureValue mustBe
        a[StcSubscriptionCreateResponse.BadRequestResponse]
    }
  }


  "EtmpClient.viewSubscription" should {

    "return SuccessResponse on 200" in {
      val body =
        """{
          |  "success": {
          |    "processingDate": "2026-04-21T09:57:03.670636Z",
          |    "subsValidTo": "2027-04-21",
          |    "contactName": "John Mill",
          |    "addressLine1": "350 But Close",
          |    "addressLine2": "Greenwich",
          |    "addressLine3": "London",
          |    "postcode": "SE10 7KGT",
          |    "countryCode": "GB",
          |    "telephoneNumber": "02081129921",
          |    "email": "john.mill@cap.com"
          |  }
          |}""".stripMargin

      stubGet(
        s"$baseUrl/RESTAdapter/stc/subscription/$subscriptionId",
        200,
        body
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.viewSubscription(subscriptionId, correlationId).futureValue mustBe
        a[StcSubscriptionViewResponse.SuccessResponse]
    }
  }


  "EtmpClient.amendSubscription" should {

    val subscription = Subscription(
      contactName = "John Mill",
      addressLine1 = "350 But Close",
      addressLine2 = None,
      addressLine3 = None,
      postcode = "SE10 7KGT",
      countryCode = "GB",
      telephoneNumber = "0777777777",
      email = "some@email.com")

    "return SuccessResponse on 200" in {
      val body =
        """{
          |  "success": {
          |    "processingDate": "2026-04-21T09:57:03.670636Z"
          |  }
          |}""".stripMargin

      stubPut(
        s"$baseUrl/RESTAdapter/stc/subscription/$subscriptionId",
        200,
        body
      )

      val client = app.injector.instanceOf[EtmpClient]

      client.amendSubscription(subscriptionId, correlationId, subscription).futureValue mustBe
        a[StcSubscriptionAmendResponse.SuccessResponse]
    }
  }
}