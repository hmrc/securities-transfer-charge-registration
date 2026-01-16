/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargeregistration.config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(
                           config: Configuration,
                           servicesConfig: ServicesConfig
                         ) {

  val appName: String =
    config.get[String]("appName")

  private val serviceName =
    "securities-transfer-charge-stubs"

  private val context =
    "securities-transfer-charge-stubs"

  private val baseUrlOverride: Option[String] =
    config.getOptional[String]("stcStubs.baseUrlOverride")

  val stcStubsBaseUrl: String =
    baseUrlOverride.getOrElse {
      s"${servicesConfig.baseUrl(serviceName)}/$context"
    }
}
