/*
 * Copyright 2024 HM Revenue & Customs
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

import config.Service
import connectors.PlatformOperatorConnector.{CreatePlatformOperatorFailure, ViewPlatformOperatorFailure}
import models.operator.responses.{PlatformOperatorCreatedResponse, ViewPlatformOperatorsResponse}
import models.operator.requests.CreatePlatformOperatorRequest
import play.api.Configuration
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PlatformOperatorConnector @Inject() (
                                            configuration: Configuration,
                                            httpClient: HttpClientV2
                                          )(implicit ec: ExecutionContext) {

  private val digitalPlatformReporting: Service = configuration.get[Service]("microservice.services.digital-platform-reporting")

  def createPlatformOperator(request: CreatePlatformOperatorRequest)
                            (implicit hc: HeaderCarrier): Future[PlatformOperatorCreatedResponse] =
    httpClient.post(url"$digitalPlatformReporting/digital-platform-reporting/platform-operator")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK     => Future.successful(response.json.as[PlatformOperatorCreatedResponse])
          case status => Future.failed(CreatePlatformOperatorFailure(status))
        }
      }

  def viewPlatformOperators(implicit hc: HeaderCarrier): Future[ViewPlatformOperatorsResponse] =
    httpClient.get(url"$digitalPlatformReporting/digital-platform-reporting/platform-operator")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(response.json.as[ViewPlatformOperatorsResponse])
          case NOT_FOUND => Future.successful(ViewPlatformOperatorsResponse(Seq.empty))
          case status    => Future.failed(ViewPlatformOperatorFailure(status))
        }
      }
}

object PlatformOperatorConnector {

  final case class CreatePlatformOperatorFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Create platform operator failed with status: $status"
  }

  final case class ViewPlatformOperatorFailure(status: Int) extends Throwable {
    override def getMessage: String = s"View platform operator failed with status: $status"
  }
}