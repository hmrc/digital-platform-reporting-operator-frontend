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
import controllers.{routes => baseRoutes}
import forms.{DueDiligenceActiveOnlyFormProvider, DueDiligenceFormProvider}
import models.operator.NotificationType
import models.operator.responses.NotificationDetails
import models.{DueDiligence, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.notification.{DueDiligencePage, ReportingPeriodPage}
import pages.update.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.NotificationDetailsQuery
import repositories.SessionRepository
import views.html.notification.{DueDiligenceActiveOnlyView, DueDiligenceView}

import java.time.Instant
import scala.concurrent.Future

class DueDiligenceControllerSpec extends SpecBase with MockitoSugar {

  private lazy val dueDiligenceRoute = routes.DueDiligenceController.onPageLoad(NormalMode, operatorId).url

  private val formProvider = new DueDiligenceFormProvider()
  private val formProviderActiveOnly = new DueDiligenceActiveOnlyFormProvider()
  private val businessName = "name"
  private val form = formProvider(businessName)
  private val formActiveOnly = formProviderActiveOnly(businessName)
  private val baseAnswers =
  emptyUserAnswers
    .set(BusinessNamePage, businessName).success.value
    .set(ReportingPeriodPage, 2025).success.value
    .set(NotificationDetailsQuery, Nil).success.value

  "DueDiligence Controller" - {

    "must return OK and the correct view for a GET" -  {

      "when there are no existing notifications" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, dueDiligenceRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DueDiligenceView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }

      "when there are only EPO notifications" in {

        val notification = NotificationDetails(NotificationType.Epo, None, None, 2024, Instant.now)
        val answers = baseAnswers.set(NotificationDetailsQuery, Seq(notification)).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, dueDiligenceRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DueDiligenceView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }

      "when all RPO notifications are on or after the reporting year of this notification" in {

        val notification = NotificationDetails(NotificationType.Rpo, None, None, 2025, Instant.now)
        val answers = baseAnswers.set(NotificationDetailsQuery, Seq(notification)).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, dueDiligenceRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DueDiligenceView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }

      "when there is an RPO notification for a year before the year of this notification" in {

        val notification = NotificationDetails(NotificationType.Rpo, None, None, 2024, Instant.now)
        val answers = baseAnswers.set(NotificationDetailsQuery, Seq(notification)).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, dueDiligenceRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DueDiligenceActiveOnlyView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" - {

      "when there isn't an RPO notification for a year earlier than the year of this notification" in {

        val userAnswers = baseAnswers.set(DueDiligencePage, Set(DueDiligence.values.head)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, dueDiligenceRoute)

          val view = application.injector.instanceOf[DueDiligenceView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(Set(DueDiligence.values.head)), NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }

      "when there is an RPO notification for a year earlier than the year of this notification" in {

        val notification = NotificationDetails(NotificationType.Rpo, None, None, 2024, Instant.now)
        val answers =
          baseAnswers
            .set(DueDiligencePage, Set[DueDiligence](DueDiligence.ActiveSeller)).success.value
            .set(NotificationDetailsQuery, Seq(notification)).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, dueDiligenceRoute)

          val view = application.injector.instanceOf[DueDiligenceActiveOnlyView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(formActiveOnly.fill(Set(DueDiligence.ActiveSeller)), NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, dueDiligenceRoute)
            .withFormUrlEncodedBody(("value[0]", DueDiligence.values.head.toString))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(DueDiligencePage, Set(DueDiligence.values.head)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual DueDiligencePage.nextPage(NormalMode, operatorId, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" - {

      "when there isn't an RPO notification for a year earlier than the year of this notification" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, dueDiligenceRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[DueDiligenceView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }

      "when there is an RPO notification for a year earlier than the year of this notification" in {

        val notification = NotificationDetails(NotificationType.Rpo, None, None, 2024, Instant.now)
        val answers = baseAnswers.set(NotificationDetailsQuery, Seq(notification)).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request =
            FakeRequest(POST, dueDiligenceRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = formActiveOnly.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[DueDiligenceActiveOnlyView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, dueDiligenceRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, dueDiligenceRoute)
            .withFormUrlEncodedBody(("value", DueDiligence.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
