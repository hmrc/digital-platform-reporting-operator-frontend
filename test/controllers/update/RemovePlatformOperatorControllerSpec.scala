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

package controllers.update

import base.SpecBase
import connectors.PlatformOperatorConnector
import forms.RemovePlatformOperatorFormProvider
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.update.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.update.RemovePlatformOperatorView

import scala.concurrent.Future

class RemovePlatformOperatorControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]

  private val formProvider = new RemovePlatformOperatorFormProvider()
  private val businessName = "name"
  private val form = formProvider(businessName)

  private val baseAnswers = emptyUserAnswers.set(BusinessNamePage, businessName).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository)
    super.beforeEach()
  }

  "RemovePlatformOperator Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemovePlatformOperatorController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemovePlatformOperatorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, operatorId, businessName)(request, messages(application)).toString
      }
    }

    "must remove the platform operator and redirect to Platform Operator Removed for a POST when the answer is yes" in {

      when(mockConnector.removePlatformOperator(any())(any())) thenReturn Future.successful(Done)
      when(mockRepository.clear(any(), any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemovePlatformOperatorController.onSubmit(operatorId).url)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PlatformOperatorRemovedController.onPageLoad(operatorId).url

        verify(mockConnector, times(1)).removePlatformOperator(eqTo(operatorId))(any())
        verify(mockRepository, times(1)).clear(eqTo("id"), eqTo(Some(operatorId)))
      }
    }

    "must redirect to Platform Operator for a POST when the answer is no" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemovePlatformOperatorController.onSubmit(operatorId).url)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PlatformOperatorController.onPageLoad(operatorId).url

        verify(mockConnector, never()).removePlatformOperator(any())(any())
        verify(mockRepository, never()).clear(any(), any())
      }
    }
  }
}
