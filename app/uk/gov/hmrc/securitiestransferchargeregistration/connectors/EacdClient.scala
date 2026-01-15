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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps}
import uk.gov.hmrc.securitiestransferchargeregistration.config.AppConfig
import uk.gov.hmrc.securitiestransferchargeregistration.models.{IndividualEnrolmentDetails, OrganisationEnrolmentDetails}
import uk.gov.hmrc.http.HttpReads.Implicits.*

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EacdClientImpl])
trait EacdClient {
  def enrolIndividual(details: IndividualEnrolmentDetails): Future[Unit]
  def enrolOrganisation(details: OrganisationEnrolmentDetails): Future[Unit]
}

@Singleton
final class EacdClientImpl @Inject()(
                                      http: HttpClientV2,
                                      appConfig: AppConfig
                                    )(implicit ec: ExecutionContext) extends EacdClient {

  private def enrolIndividualUrl =
    url"${appConfig.stcStubsBaseUrl}/enrolment/individual"

  private def enrolOrganisationUrl =
    url"${appConfig.stcStubsBaseUrl}/enrolment/organisation"

  override def enrolIndividual(details: IndividualEnrolmentDetails): Future[Unit] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    http
      .post(enrolIndividualUrl)
      .withBody(Json.toJson(details))
      .execute[HttpResponse]
      .map { resp =>
        resp.status match {
          case Status.NO_CONTENT | Status.OK =>
            ()

          case other =>
            throw new RuntimeException(s"EACD enrolIndividual unexpected status=$other body=${resp.body}")
        }
      }
      .recoverWith {
        case e: HttpException if e.responseCode == Status.BAD_REQUEST =>
          Future.failed(new RuntimeException(s"EACD enrolIndividual failed: 400 ${e.message}"))
      }
  }

  override def enrolOrganisation(details: OrganisationEnrolmentDetails): Future[Unit] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    http
      .post(enrolOrganisationUrl)
      .withBody(Json.toJson(details))
      .execute[HttpResponse]
      .map { resp =>
        resp.status match {
          case Status.NO_CONTENT | Status.OK =>
            ()

          case other =>
            throw new RuntimeException(s"EACD enrolOrganisation unexpected status=$other body=${resp.body}")
        }
      }
      .recoverWith {
        case e: HttpException if e.responseCode == Status.BAD_REQUEST =>
          Future.failed(new RuntimeException(s"EACD enrolOrganisation failed: 400 ${e.message}"))
      }
  }
}
