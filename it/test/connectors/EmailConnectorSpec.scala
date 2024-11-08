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

import base.ConnectorSpecBase
import builders.SendEmailRequestBuilder.aSendEmailRequest
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class EmailConnectorSpec extends ConnectorSpecBase {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure("microservice.services.email.port" -> wireMockPort)
    .build()

  private lazy val underTest = app.injector.instanceOf[EmailConnector]

  ".send" - {
    "must return true when when the server returns ACCEPTED" in {
      wireMockServer.stubFor(post(urlMatching("/hmrc/email"))
        .willReturn(aResponse.withStatus(202)))

      underTest.send(aSendEmailRequest).futureValue mustBe true
    }

    "must return false when the server returns an error response" in {
      wireMockServer.stubFor(post(urlMatching("/hmrc/email"))
        .willReturn(badRequest()))

      underTest.send(aSendEmailRequest).futureValue mustBe false
    }

    "must return false when sending email results in exception" in {
      wireMockServer.stubFor(post(urlMatching("/hmrc/email"))
        .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)))

      underTest.send(aSendEmailRequest).futureValue mustBe false
    }
  }
}
