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

package controllers

import base.SpecBase
import connectors.PlatformOperatorConnector
import models.operator.responses.ViewPlatformOperatorsResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.PlatformOperatorsViewModel
import views.html.PlatformOperatorsView

import scala.concurrent.Future

class PlatformOperatorsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
    super.beforeEach()
  }

  "PlatformOperators Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockConnector.viewPlatformOperators(any())) thenReturn Future.successful(ViewPlatformOperatorsResponse(Seq.empty))

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.PlatformOperatorsController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PlatformOperatorsView]
        val viewModel = PlatformOperatorsViewModel(Seq.empty)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
      }
    }
  }
}
