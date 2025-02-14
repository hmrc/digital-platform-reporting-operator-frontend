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
import builders.PlatformOperatorSummaryViewModelBuilder.aPlatformOperatorSummaryViewModel
import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import connectors.SubscriptionConnector
import models.email.EmailsSentResult
import models.pageviews.PlatformOperatorRemovedViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{PlatformOperatorDeletedQuery, SentRemovedPlatformOperatorEmailQuery}
import views.html.update.PlatformOperatorRemovedView

import scala.concurrent.Future

class PlatformOperatorRemovedControllerSpec extends SpecBase with MockitoSugar {

  private val mockConnector = mock[SubscriptionConnector]

  "PlatformOperatorRemoved Controller" - {

    "must return OK and the correct view for a GET" - {
      "for different emails" in {
        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(aSubscriptionInfo)

        val emailsSentResult = EmailsSentResult(userEmailSent = true, Some(true))
        val baseAnswers = emptyUserAnswers.set(PlatformOperatorDeletedQuery, aPlatformOperatorSummaryViewModel).success.value
          .set(SentRemovedPlatformOperatorEmailQuery, emailsSentResult).success.value

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          inject.bind[SubscriptionConnector].toInstance(mockConnector)).build()

        running(application) {
          val request = FakeRequest(GET, routes.PlatformOperatorRemovedController.onPageLoad(operatorId).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[PlatformOperatorRemovedView]

          val viewModel = PlatformOperatorRemovedViewModel(aSubscriptionInfo, aPlatformOperatorSummaryViewModel,
            emailsSentResult)
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
          contentAsString(result) must include(
            messages(application)("platformOperatorRemoved.p1.1.two.emails", viewModel.userEmail, viewModel.poEmail)
          )
        }
      }

      "for same emails" in {
        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(aSubscriptionInfo)

        val emailsSentResult = EmailsSentResult(userEmailSent = true, poEmailSent = None)
        val baseAnswers = emptyUserAnswers.set(PlatformOperatorDeletedQuery, aPlatformOperatorSummaryViewModel).success.value
          .set(SentRemovedPlatformOperatorEmailQuery, emailsSentResult).success.value

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          inject.bind[SubscriptionConnector].toInstance(mockConnector)).build

        running(application) {
          val request = FakeRequest(GET, routes.PlatformOperatorRemovedController.onPageLoad(operatorId).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[PlatformOperatorRemovedView]

          val platformOperatorViewModel = aPlatformOperatorSummaryViewModel.copy(poPrimaryContactEmail = aSubscriptionInfo.primaryContact.email)
          val viewModel = PlatformOperatorRemovedViewModel(aSubscriptionInfo, platformOperatorViewModel,
            emailsSentResult)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
          contentAsString(result) must include(
            messages(application)("platformOperatorRemoved.p1.1.one.email", viewModel.userEmail, viewModel.poEmail)
          )
        }
      }

      "when no emails were sent" in {
        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(aSubscriptionInfo)

        val emailsSentResult = EmailsSentResult(userEmailSent = false, poEmailSent = None)
        val baseAnswers = emptyUserAnswers.set(PlatformOperatorDeletedQuery, aPlatformOperatorSummaryViewModel).success.value
          .set(SentRemovedPlatformOperatorEmailQuery, emailsSentResult).success.value

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          inject.bind[SubscriptionConnector].toInstance(mockConnector)).build

        running(application) {
          val request = FakeRequest(GET, routes.PlatformOperatorRemovedController.onPageLoad(operatorId).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[PlatformOperatorRemovedView]

          val viewModel = PlatformOperatorRemovedViewModel(aSubscriptionInfo, aPlatformOperatorSummaryViewModel,
            emailsSentResult)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
          contentAsString(result) must include(
            messages(application)("platformOperatorRemoved.emailNotSent.warning", viewModel.userEmail, viewModel.poEmail)
          )
        }
      }
    }
  }
}
