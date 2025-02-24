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
import builders.PlatformOperatorBuilder.aPlatformOperator
import connectors.{PlatformOperatorConnector, SubmissionsConnector}
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.add.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import viewmodels.PlatformOperatorViewModel
import views.html.update.PlatformOperatorView

import scala.concurrent.Future

class PlatformOperatorControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockSubmissionsConnector = mock[SubmissionsConnector]
  private val mockRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    reset(mockPlatformOperatorConnector, mockSubmissionsConnector, mockRepository)
    super.beforeEach()
  }

  "Platform Operator controller" - {
    "must get platform operator details, save them, and show the correct view" in {
      when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())) thenReturn Future.successful(aPlatformOperator)
      when(mockSubmissionsConnector.submissionsExist(any)(any)) thenReturn Future.successful(true)
      when(mockSubmissionsConnector.assumedReportsExist(any)(any)) thenReturn Future.successful(true)
      when(mockRepository.set(any())) thenReturn Future.successful(true)

      val app = applicationBuilder(userAnswers = None).overrides(
        bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
        bind[SubmissionsConnector].toInstance(mockSubmissionsConnector),
        bind[SessionRepository].toInstance(mockRepository)
      ).build()

      running(app) {
        val request = FakeRequest(GET, routes.PlatformOperatorController.onPageLoad(aPlatformOperator.operatorId).url)
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        val view = app.injector.instanceOf[PlatformOperatorView]
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(PlatformOperatorViewModel(aPlatformOperator, hasSubmissions = true, hasAssumedReports = true))(request, messages(app)).toString

        verify(mockPlatformOperatorConnector, times(1)).viewPlatformOperator(eqTo(aPlatformOperator.operatorId))(any())
        verify(mockRepository, times(1)).set(answersCaptor.capture())

        val answers = answersCaptor.getValue
        answers.operatorId.value mustEqual aPlatformOperator.operatorId
        answers.get(BusinessNamePage).value mustEqual aPlatformOperator.operatorName
      }
    }
  }
}
