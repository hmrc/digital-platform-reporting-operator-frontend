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

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.PlatformOperatorConnector.CreatePlatformOperatorFailure
import models.operator.{AddressDetails, ContactDetails, PlatformOperatorCreatedResponse}
import models.operator.requests.CreatePlatformOperatorRequest
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.http.test.WireMockSupport

class PlatformOperatorConnectorSpec extends AnyFreeSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with WireMockSupport
  with MockitoSugar
  with BeforeAndAfterEach
  with EitherValues {

  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.digital-platform-reporting.port" -> wireMockPort)
    .build()

  private lazy val connector = app.injector.instanceOf[PlatformOperatorConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("authToken")))

  ".createPlatformOperator" - {
    "must post a request" - {
      "and return the response when the server returns OK" in {

        val request = CreatePlatformOperatorRequest(subscriptionId = "dprs is",
          operatorName = "name",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, None, None)
        )
        val serverResponse = PlatformOperatorCreatedResponse("operator id")

        wireMockServer.stubFor(
          post(urlPathEqualTo("/digital-platform-reporting/platform-operator"))
            .withHeader("Authorization", equalTo("authToken"))
            .withRequestBody(equalTo(Json.toJson(request).toString))
            .willReturn(
              ok(Json.toJson(serverResponse).toString)
            )
        )

        val result = connector.createPlatformOperator(request).futureValue

        result mustEqual serverResponse
      }

      "and return a failed future when the server returns an error" in {

        val request = CreatePlatformOperatorRequest(subscriptionId = "dprs is",
          operatorName = "name",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, None, None)
        )

        wireMockServer.stubFor(
          post(urlPathEqualTo("/digital-platform-reporting/platform-operator"))
            .withHeader("Authorization", equalTo("authToken"))
            .withRequestBody(equalTo(Json.toJson(request).toString))
            .willReturn(
              serverError()
            )
        )

        val result = connector.createPlatformOperator(request).failed.futureValue
        result mustBe a[CreatePlatformOperatorFailure]

        val failure = result.asInstanceOf[CreatePlatformOperatorFailure]
        failure.status mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }
}
