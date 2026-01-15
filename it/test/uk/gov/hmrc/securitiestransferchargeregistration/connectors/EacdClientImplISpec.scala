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
import uk.gov.hmrc.securitiestransferchargeregistration.models.IndividualEnrolmentDetails
import uk.gov.hmrc.securitiestransferchargeregistration.support.WireMockISpecBase

class EacdClientImplISpec
  extends WireMockISpecBase
    with ScalaFutures {

  "EacdClientImpl.enrolIndividual" should {

    "succeed on 204 NO_CONTENT" in {
      wireMock.stubFor(
        post(urlEqualTo("/securities-transfer-charge-stubs/enrolment/individual"))
          .willReturn(aResponse().withStatus(204))
      )

      val client = app.injector.instanceOf[EacdClient]

      val details = IndividualEnrolmentDetails(
        subscriptionId = "SUB123456",
        nino = "AB123456C"
      )

      client.enrolIndividual(details).futureValue mustBe ()
    }

    "fail on 400 BAD_REQUEST" in {
      wireMock.stubFor(
        post(urlEqualTo("/securities-transfer-charge-stubs/enrolment/individual"))
          .willReturn(
            aResponse()
              .withStatus(400)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"code":"INVALID_PAYLOAD"}""")
          )
      )

      val client = app.injector.instanceOf[EacdClient]

      val details = IndividualEnrolmentDetails(
        subscriptionId = "SUB123456",
        nino = "AB123456C"
      )

      val ex = intercept[RuntimeException] {
        client.enrolIndividual(details).futureValue
      }

      ex.getMessage must include("400")
    }
  }
}
