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

package controllers.notification

import base.SpecBase
import builders.PlatformOperatorBuilder.aPlatformOperator
import connectors.PlatformOperatorConnector
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.notification.AddGuidancePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.notification.AddGuidanceView

import scala.concurrent.Future

class AddGuidanceControllerSpec extends SpecBase with MockitoSugar {

  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]

  "Start Controller" - {

    "must get platform operator details, save them, and show the correct view" in {

      when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())) thenReturn Future.successful(aPlatformOperator)
      when(mockRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.AddGuidanceController.onPageLoad(aPlatformOperator.operatorId).url)
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddGuidanceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(aPlatformOperator.operatorId, aPlatformOperator.operatorName)(request, messages(application)).toString

        verify(mockPlatformOperatorConnector, times(1)).viewPlatformOperator(eqTo(aPlatformOperator.operatorId))(any())
        verify(mockRepository, times(1)).set(answersCaptor.capture())

      }
    }

    "must redirect to the next page for a POST" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.AddGuidanceController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual AddGuidancePage.nextPage(NormalMode, operatorId, emptyUserAnswers).url
      }
    }
  }
}
