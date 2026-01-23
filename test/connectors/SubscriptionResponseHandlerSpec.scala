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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.securitiestransferchargeregistration.connectors.{SubscriptionErrorException, SubscriptionResponseHandler, SubscriptionResponseParseError}

import scala.concurrent.Future

class SubscriptionResponseHandlerSpec
  extends AnyWordSpec
    with MockitoSugar
    with Matchers
    with ScalaFutures {

  private def mockResponse(statusCode: Int, body: String): HttpResponse = {
    val resp = mock[HttpResponse]
    when(resp.status).thenReturn(statusCode)
    when(resp.body).thenReturn(body)
    resp
  }

  "SubscriptionResponseHandler.handle" should {

    "return stcId when the status is 200 and the JSON response is valid" in {
      val body =
        """{
          |  "success": {
          |    "processingDate": "2025-01-10T09:30:47Z",
          |    "stcId": "XASTS0123456789"
          |  }
          |}""".stripMargin

      val response = mockResponse(201, body)

      val result: Future[String] = SubscriptionResponseHandler.handle(response)

      result.futureValue mustBe "XASTS0123456789"
    }

    "fail with SubscriptionResponseParseError when the status is 200 but JSON response is invalid" in {
      val invalidBody =
        """{
          |  "success": {
          |    "processingDate": "2025-01-10T09:30:47Z"
          |  }
          |}""".stripMargin

      val response = mockResponse(201, invalidBody)

      val result: Future[String] = SubscriptionResponseHandler.handle(response)

      whenReady(result.failed) { ex =>
        ex mustBe a[SubscriptionResponseParseError]
        ex.getMessage must include("Invalid success response JSON received")
      }
    }

    "fail with SubscriptionErrorException for non-200 responses (e.g., 400, 401, 403, 404, 500)" in {
      val testCases = Seq(
        400 -> "Bad Request",
        401 -> "Unauthorized",
        403 -> "Forbidden",
        404 -> "Not Found",
        500 -> "Internal Server Error"
      )

      testCases.foreach { case (status, _) =>
        val body = s"""{"error": "status $status"}"""
        val response = mockResponse(status, body)
        val result = SubscriptionResponseHandler.handle(response)

        whenReady(result.failed) { ex =>
          ex mustBe a[SubscriptionErrorException]
          ex.getMessage must include(status.toString)
        }
      }
    }
  }
}


