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
import connectors.PlatformOperatorConnector._
import models.operator.{AddressDetails, ContactDetails}
import models.operator.requests._
import models.operator.responses._
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

  ".updatePlatformOperator" - {

    "must post a request" - {

      "and succeed when the server returns OK" in {

        val request = UpdatePlatformOperatorRequest(subscriptionId = "dprs id",
          operatorId = "operatorId",
          operatorName = "name",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, None, None),
          notification = None
        )

        wireMockServer.stubFor(
          post(urlPathEqualTo("/digital-platform-reporting/platform-operator/operatorId"))
            .withHeader("Authorization", equalTo("authToken"))
            .withRequestBody(equalTo(Json.toJson(request).toString))
            .willReturn(ok())
        )

        connector.updatePlatformOperator(request).futureValue
      }

      "and return a failed future when the server returns an error" in {

        val request = UpdatePlatformOperatorRequest(subscriptionId = "dprs is",
          operatorId = "operatorId",
          operatorName = "name",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, None, None),
          notification = None
        )

        wireMockServer.stubFor(
          post(urlPathEqualTo("/digital-platform-reporting/platform-operator/operatorId"))
            .withHeader("Authorization", equalTo("authToken"))
            .withRequestBody(equalTo(Json.toJson(request).toString))
            .willReturn(
              serverError()
            )
        )

        val result = connector.updatePlatformOperator(request).failed.futureValue
        result mustBe an[UpdatePlatformOperatorFailure]

        val failure = result.asInstanceOf[UpdatePlatformOperatorFailure]
        failure.status mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }

  ".viewPlatformOperators" - {

    "must return platform operator details when the server returns OK" in {

      val serverResponse = ViewPlatformOperatorsResponse(platformOperators = Seq(
        PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "primaryContactName", "primaryEmail"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line1", None, None, None, Some("postCode"), None),
          notifications = Seq.empty
        )
      ))

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/platform-operator"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(
            ok(Json.toJson(serverResponse).toString)
          )
      )

      val result = connector.viewPlatformOperators.futureValue
      result mustEqual serverResponse
    }

    "must return empty platform operator details when the server returns NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/platform-operator"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(notFound())
      )

      val result = connector.viewPlatformOperators.futureValue
      result mustEqual ViewPlatformOperatorsResponse(Seq.empty)
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/platform-operator"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(serverError())
      )

      val result = connector.viewPlatformOperators.failed.futureValue
      result mustBe a[ViewPlatformOperatorFailure]

      val failure = result.asInstanceOf[ViewPlatformOperatorFailure]
      failure.status mustEqual INTERNAL_SERVER_ERROR
    }
  }

  ".viewPlatformOperator" - {

    "must return a platform operator's details when the server returns OK" in {

      val serverResponse =
        PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "primaryContactName", "primaryEmail"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line1", None, None, None, Some("postCode"), None),
          notifications = Seq.empty
        )

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/platform-operator/operatorId"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(
            ok(Json.toJson(serverResponse).toString)
          )
      )

      val result = connector.viewPlatformOperator("operatorId").futureValue
      result mustEqual serverResponse
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/platform-operator/operatorId"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(
            serverError()
          )
      )

      val result = connector.viewPlatformOperator("operatorId").failed.futureValue
      result mustBe a[ViewPlatformOperatorFailure]

      val failure = result.asInstanceOf[ViewPlatformOperatorFailure]
      failure.status mustEqual 500
    }
  }

  ".removePlatformOperator" - {

    "must return successfully when the server returns OK" in {

      wireMockServer.stubFor(
        delete(urlPathEqualTo("/digital-platform-reporting/platform-operator/operatorId"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(ok())
      )

      connector.removePlatformOperator("operatorId").futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        delete(urlPathEqualTo("/digital-platform-reporting/platform-operator/operatorId"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(serverError())
      )

      val result = connector.removePlatformOperator("operatorId").failed.futureValue
      result mustBe a[RemovePlatformOperatorFailure]

      val failure = result.asInstanceOf[RemovePlatformOperatorFailure]
      failure.status mustEqual 500
    }
  }
}
