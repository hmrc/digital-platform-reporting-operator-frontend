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
import forms.ReportingPeriodFormProvider
import models.NormalMode
import models.operator.NotificationType
import models.operator.responses.NotificationDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.notification.{NotificationTypePage, ReportingPeriodPage}
import pages.update.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.NotificationDetailsQuery
import repositories.SessionRepository
import views.html.notification.{ReportingPeriodFirstView, ReportingPeriodView}

import java.time.{Clock, Instant, ZoneId, ZoneOffset}
import scala.concurrent.Future

class ReportingPeriodControllerSpec extends SpecBase with MockitoSugar {

  private lazy val reportingPeriodRoute = routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId).url

  private val instant = Instant.now()
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val formProvider = new ReportingPeriodFormProvider(stubClock)
  private val businessName = "name"
  private val form = formProvider(businessName)
  private val notificationType = models.NotificationType.Rpo
  private val baseAnswers =
    emptyUserAnswers
      .set(BusinessNamePage, businessName).success.value
      .set(NotificationDetailsQuery, Nil).success.value
      .set(NotificationTypePage, notificationType).success.value

  "ReportingPeriod Controller" - {

    "must return OK and the correct view for a GET" - {

      "when no notifications have been set up yet" in {

        val year = Gen.choose(2025, 2050).sample.value
        val fixedClock = Clock.fixed(Instant.parse(s"$year-12-31T00:00:00Z"), ZoneOffset.UTC)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[Clock].toInstance(fixedClock))
            .build()

        running(application) {
          val request = FakeRequest(GET, reportingPeriodRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ReportingPeriodFirstView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, NormalMode, operatorId, businessName, notificationType)(request, messages(application)).toString
        }
      }

      "when at least one notification has been set up" in {

        val year = Gen.choose(2025, 2050).sample.value
        val fixedClock = Clock.fixed(Instant.parse(s"$year-12-31T00:00:00Z"), ZoneOffset.UTC)

        val notification = NotificationDetails(NotificationType.Epo, None, None, 2024, Instant.now)
        val answers = baseAnswers.set(NotificationDetailsQuery, Seq(notification)).success.value

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[Clock].toInstance(fixedClock))
            .build()

        running(application) {
          val request = FakeRequest(GET, reportingPeriodRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ReportingPeriodView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" - {

      "when no notifications have been set up yet" in {

        val year = Gen.choose(2025, 2050).sample.value
        val fixedClock = Clock.fixed(Instant.parse(s"$year-12-31T00:00:00Z"), ZoneOffset.UTC)

        val userAnswers = baseAnswers.set(ReportingPeriodPage, 2024).success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[Clock].toInstance(fixedClock))
            .build()

        running(application) {
          val request = FakeRequest(GET, reportingPeriodRoute)

          val view = application.injector.instanceOf[ReportingPeriodFirstView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(2024), NormalMode, operatorId, businessName, notificationType)(request, messages(application)).toString
        }
      }

      "when at least one notification has been set up" in {

        val year = Gen.choose(2025, 2050).sample.value
        val fixedClock = Clock.fixed(Instant.parse(s"$year-12-31T00:00:00Z"), ZoneOffset.UTC)

        val notification = NotificationDetails(NotificationType.Epo, None, None, 2024, Instant.now)
        val userAnswers =
          baseAnswers
            .set(ReportingPeriodPage, 2024).success.value
            .set(NotificationDetailsQuery, Seq(notification)).success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[Clock].toInstance(fixedClock))
            .build()

        running(application) {
          val request = FakeRequest(GET, reportingPeriodRoute)

          val view = application.injector.instanceOf[ReportingPeriodView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(2024), NormalMode, operatorId, businessName)(request, messages(application)).toString
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
          FakeRequest(POST, reportingPeriodRoute)
            .withFormUrlEncodedBody(("value", "2024"))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(ReportingPeriodPage, 2024).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual ReportingPeriodPage.nextPage(NormalMode, operatorId, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" - {

      "when no notification have been set up" in {

        val year = Gen.choose(2025, 2050).sample.value
        val fixedClock = Clock.fixed(Instant.parse(s"$year-12-31T00:00:00Z"), ZoneOffset.UTC)
        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[Clock].toInstance(fixedClock))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, reportingPeriodRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[ReportingPeriodFirstView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, operatorId, businessName, notificationType)(request, messages(application)).toString
        }
      }

      "when at least one notification has been set up" in {

        val year = Gen.choose(2025, 2050).sample.value
        val fixedClock = Clock.fixed(Instant.parse(s"$year-12-31T00:00:00Z"), ZoneOffset.UTC)
        val notification = NotificationDetails(NotificationType.Epo, None, None, 2024, Instant.now)
        val answers = baseAnswers.set(NotificationDetailsQuery, Seq(notification)).success.value

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[Clock].toInstance(fixedClock))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, reportingPeriodRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[ReportingPeriodView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, operatorId, businessName)(request, messages(application)).toString
        }
      }

    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, reportingPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, reportingPeriodRoute)
            .withFormUrlEncodedBody(("value", "2024"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
