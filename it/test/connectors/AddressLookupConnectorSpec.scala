/*
 * Copyright 2023 HM Revenue & Customs
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

import base.ConnectorSpecBase
import builders.AddressLookupBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, getRequestedFor, post, get, postRequestedFor, stubFor, urlEqualTo}
import connectors.httpParsers.AddressLookupInitializationHttpParser.{AddressLookupInitializationResponse, AddressLookupOnRamp}
import connectors.httpParsers.ConfirmedAddressHttpParser.ConfirmedAddressResponse
import connectors.httpParsers.{AddressMalformed, AddressNotFound, DefaultedUnexpectedFailure, NoLocationHeaderReturned}
import play.api.Application
import play.api.http.Status._
import play.api.i18n.Lang
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class AddressLookupConnectorSpec extends ConnectorSpecBase {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        conf = "microservice.services.address-lookup-frontend.port" -> wireMockPort
      )
      .build()

  private lazy val connector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]

  private implicit val language: Lang = Lang("en")


  ".initialise(...)" - {

    "for a successful response" - {

      "return a Right(success response)" in {
        wireMockServer.stubFor(
          post(urlEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(ACCEPTED)
                .withHeader("Location", "/api/v2/location")
            )
        )
        val expectedResult = Right(AddressLookupOnRamp("/api/v2/location"))
        val actualResult: AddressLookupInitializationResponse =
          Await.result(connector.initialise(continueUrl = "", accessibilityFooterUrl = ""), 500.millisecond)
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/init")))
        actualResult mustBe expectedResult
      }

    }

    "for an error response" - {

      "return Left(NoLocationHeaderReturned) when there is no Location returned" in {
        wireMockServer.stubFor(
          post(urlEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(ACCEPTED)
                .withBody("")
                .withHeader("notLocation", "")
            )
        )
        val expectedResult = Left(NoLocationHeaderReturned)
        val actualResult: AddressLookupInitializationResponse =
          Await.result(connector.initialise(continueUrl = "", accessibilityFooterUrl = ""), 500.millisecond)
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/init")))
        actualResult mustBe expectedResult
      }

      "return a Left(DefaultedUnexpectedFailure) when unexpected status code" in {
        wireMockServer.stubFor(
          post(urlEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(NO_CONTENT)
            )
        )
        val expectedResult = Left(DefaultedUnexpectedFailure(NO_CONTENT))
        val actualResult: AddressLookupInitializationResponse =
          Await.result(connector.initialise(continueUrl = "", accessibilityFooterUrl = ""), 500.millisecond)
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/init")))
        actualResult mustBe expectedResult
      }

    }
  }

  ".getAddress(...)" - {

    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    "for a successful response" - {

      "return a Right(AddressModel)" in {
        wireMockServer.stubFor(
          get(urlEqualTo("/api/confirmed?id=123456789"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.obj("address" -> Json.toJson(AddressLookupBuilder.address)).toString())
            )
        )
        val expectedResult = Right(AddressLookupBuilder.address)
        val actualResult: ConfirmedAddressResponse =
          Await.result(connector.getAddress("123456789")(hc, ec), 500.millisecond)
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/confirmed?id=123456789")))
        actualResult mustBe expectedResult
      }

    }

    "for an error response" - {

      "return a Left(AddressMalformed)" in {
        stubFor(
          get(urlEqualTo("/api/confirmed?id=123456789"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(AddressLookupBuilder.address).toString())
            )
        )
        val expectedResult = Left(AddressMalformed)
        val actualResult: ConfirmedAddressResponse =
          Await.result(connector.getAddress("123456789")(hc, ec), 500.millisecond)
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/confirmed?id=123456789")))
        actualResult mustBe expectedResult
      }

      "return Left(AddressNotFound) when address couldn't found" in {
        stubFor(
          get(urlEqualTo("/api/confirmed?id=123456789"))
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )
        val expectedResult = Left(AddressNotFound)
        val actualResult = Await.result(connector.getAddress("123456789")(hc, ec), 500.millisecond)
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/confirmed?id=123456789")))
        actualResult mustBe expectedResult
      }

      "return a Left(DefaultedUnexpectedFailure) when unexpected response" in {
        stubFor(
          get(urlEqualTo("/api/confirmed?id=123456789"))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )
        val expectedResult = Left(DefaultedUnexpectedFailure(INTERNAL_SERVER_ERROR))
        val actualResult = Await.result(connector.getAddress("123456789")(hc, ec), 500.millisecond)
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/confirmed?id=123456789")))
        actualResult mustBe expectedResult
      }

    }

  }
    
}

