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

package controllers.add

import base.SpecBase
import builders.EmailsSentResultBuilder.anEmailsSentResult
import builders.PlatformOperatorSummaryViewModelBuilder.aPlatformOperatorSummaryViewModel
import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import connectors.SubscriptionConnector
import models.email.EmailsSentResult
import models.pageviews.PlatformOperatorAddedViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{PlatformOperatorAddedQuery, SentAddedPlatformOperatorEmailQuery}
import repositories.SessionRepository
import views.html.add.PlatformOperatorAddedView

import scala.concurrent.Future


class PlatformOperatorAddedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[SubscriptionConnector]
  private val mockRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    reset(mockConnector, mockRepository)
    super.beforeEach()
  }

  "PlatformOperatorAdded Controller" - {
    "must return OK and the correct view for GET" - {
      "for different emails" in {
        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(aSubscriptionInfo)
        when(mockRepository.set(any())) thenReturn Future.successful(true)

        val emailsSentResult = EmailsSentResult(userEmailSent = true, Some(true))
        val baseAnswers = emptyUserAnswers
          .set(PlatformOperatorAddedQuery, aPlatformOperatorSummaryViewModel).success.value
          .set(SentAddedPlatformOperatorEmailQuery, emailsSentResult).success.value

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[SubscriptionConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository)
        ).build

        running(application) {
          val request = FakeRequest(GET, routes.PlatformOperatorAddedController.onPageLoad.url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[PlatformOperatorAddedView]

          status(result) mustEqual OK
          val viewModel = PlatformOperatorAddedViewModel(aSubscriptionInfo, aPlatformOperatorSummaryViewModel,
            emailsSentResult)
          contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString()
          contentAsString(result) must include(
            messages(application)("platformOperatorAdded.p1.2.two.emails", viewModel.userEmail, viewModel.poEmail)
          )
        }
      }

      "for same emails" in {
        val platformOperatorViewModel = aPlatformOperatorSummaryViewModel.copy(poPrimaryContactEmail = aSubscriptionInfo.primaryContact.email)
        val baseAnswers = emptyUserAnswers
          .set(PlatformOperatorAddedQuery, platformOperatorViewModel).success.value
          .set(SentAddedPlatformOperatorEmailQuery, anEmailsSentResult).success.value

        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(aSubscriptionInfo)
        when(mockRepository.set(any())) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[SubscriptionConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository)
        ).build

        running(application) {
          val request = FakeRequest(GET, routes.PlatformOperatorAddedController.onPageLoad.url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[PlatformOperatorAddedView]

          status(result) mustEqual OK
          val viewModel = PlatformOperatorAddedViewModel(aSubscriptionInfo, platformOperatorViewModel, anEmailsSentResult)
          contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
          contentAsString(result) must include(messages(application)("platformOperatorAdded.p1.2.one.email", aSubscriptionInfo.primaryContact.email))
        }
      }

      "when no emails were sent is false" in {
        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(aSubscriptionInfo)
        when(mockRepository.set(any())) thenReturn Future.successful(true)

        val emailsSentResult = anEmailsSentResult.copy(userEmailSent = false, poEmailSent = None)
        val baseAnswers = emptyUserAnswers
          .set(PlatformOperatorAddedQuery, aPlatformOperatorSummaryViewModel).success.value
          .set(SentAddedPlatformOperatorEmailQuery, emailsSentResult).success.value

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[SubscriptionConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository)
        ).build

        running(application) {
          val request = FakeRequest(GET, routes.PlatformOperatorAddedController.onPageLoad.url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[PlatformOperatorAddedView]

          status(result) mustEqual OK
          val viewModel = PlatformOperatorAddedViewModel(aSubscriptionInfo, aPlatformOperatorSummaryViewModel, emailsSentResult)
          contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
          contentAsString(result) must include(messages(application)("platformOperatorAdded.emailNotSent.warning"))
        }
      }
    }
  }
}
