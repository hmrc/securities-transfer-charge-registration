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

package connectors


import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.securitiestransferchargeregistration.connectors.SubscriptionResponseHandler
import uk.gov.hmrc.securitiestransferchargeregistration.models.SubscriptionFailure

class SubscriptionResponseHandlerSpec extends AnyWordSpec with MockitoSugar with Matchers {

  private def mockResponse(statusCode: Int, body: String): HttpResponse = {
    val resp = mock[HttpResponse]
    when(resp.status).thenReturn(statusCode)
    when(resp.body).thenReturn(body)
    resp
  }

  "SubscriptionResponseHandler.handle" should {

    "return Right(stcId) when the status is 200 and the JSON response is valid" in {
      val body =
        """{
          |  "success": {
          |    "processingDate": "2025-01-10T09:30:47Z",
          |    "stcId": "XASTS0123456789"
          |  }
          |}""".stripMargin

      val response = mockResponse(200, body)

      SubscriptionResponseHandler.handle(response) mustBe Right("XASTS0123456789")
    }

    "return Left(InvalidSuccessResponse) when the status is 200 but the JSON response fails to parse" in {
      val invalidBody =
        """{
          |  "success": {
          |    "processingDate": "2025-01-10T09:30:47Z"
          |  }
          |}""".stripMargin

      val response = mockResponse(200, invalidBody)

      SubscriptionResponseHandler.handle(response) mustBe Left(SubscriptionFailure.InvalidSuccessResponse(invalidBody))
    }

    "return Left(Unauthorized) when the status is 401" in {
      val response = mockResponse(401, """{"any":"body"}""")

      SubscriptionResponseHandler.handle(response) mustBe Left(SubscriptionFailure.Unauthorized)
    }

    "return Left(Forbidden) when the status is 403" in {
      val response = mockResponse(403, """{"any":"body"}""")

      SubscriptionResponseHandler.handle(response) mustBe Left(SubscriptionFailure.Forbidden)
    }

    "return Left(NotFound) when the status is 404" in {
      val response = mockResponse(404, """{"any":"body"}""")

      SubscriptionResponseHandler.handle(response) mustBe Left(SubscriptionFailure.NotFound)
    }

    "return Left(SubscriptionFailure.InvalidErrorResponse) when the status is 400" in {
      val responseBody = """{
                       |  "error": {
                       |    "code": "400",
                       |    "message": "string",
                       |    "logID": "CFBE36D42271A9168125B7B5E6EB54B1"
                       |  }
                       |}""".stripMargin

      val response = mockResponse(400, responseBody)

      SubscriptionResponseHandler.handle(response) mustBe Left(SubscriptionFailure.InvalidErrorResponse(400,responseBody))
    }

    "return Left(UnexpectedStatus) for any other status (e.g., 500)" in {
      val body =
        """{
          |  "error": {
          |    "code": "500",
          |    "message": "Internal server error",
          |    "logID": "5A25056E296423F68695903376F6E59A"
          |  }
          |}""".stripMargin

      val response = mockResponse(500, body)

      SubscriptionResponseHandler.handle(response) mustBe Left(SubscriptionFailure.UnexpectedStatus(500, body))
    }
  }
}
