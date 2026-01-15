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

package uk.gov.hmrc.securitiestransferchargeregistration.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.inject.guice.GuiceApplicationBuilder

trait WireMockISpecBase
  extends ISpecBase
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  protected lazy val wireMock: WireMockServer =
    new WireMockServer(wireMockConfig().dynamicPort())

  protected final def stubsBaseUrlOverride: String =
    s"http://localhost:${wireMock.port()}/securities-transfer-charge-stubs"

  override protected def appBuilder: GuiceApplicationBuilder =
    super.appBuilder
      .configure("stcStubs.baseUrlOverride" -> stubsBaseUrlOverride)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.start()
    configureFor("localhost", wireMock.port())
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    try wireMock.stop()
    finally super.afterAll()
  }
}
