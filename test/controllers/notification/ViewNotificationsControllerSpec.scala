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
import forms.{ReportingPeriodFormProvider, ViewNotificationsFormProvider}
import models.NormalMode
import models.operator.NotificationType
import models.operator.responses.NotificationDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.notification.{ReportingPeriodPage, ViewNotificationsPage}
import pages.update.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.NotificationDetailsQuery
import repositories.SessionRepository
import views.html.notification.{ReportingPeriod2024View, ReportingPeriodFirstView, ReportingPeriodView, ViewNotificationsView}

import java.time.{Clock, Instant, ZoneId, ZoneOffset}
import scala.concurrent.Future

class ViewNotificationsControllerSpec extends SpecBase with MockitoSugar {

  private lazy val viewNotificationsRoute = routes.ViewNotificationsController.onPageLoad(operatorId).url
  private val formProvider = new ViewNotificationsFormProvider()
  private val businessName = "name"
  private val baseAnswers =
    emptyUserAnswers
      .set(BusinessNamePage, businessName).success.value
      .set(NotificationDetailsQuery, Nil).success.value

  "ViewNotifications Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, viewNotificationsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewNotificationsView]
        val form = formProvider(false)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, Nil, operatorId, businessName)(request, messages(application)).toString
      }
    }

    "must redirect to the next page for a POST with valid data" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, viewNotificationsRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        val expectedAnswers = baseAnswers.set(ViewNotificationsPage, true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual ViewNotificationsPage.nextPage(NormalMode, operatorId, expectedAnswers).url
      }
    }

    "must return a Bad Request and show the correct view for a POST with invalid data" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, viewNotificationsRoute).withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewNotificationsView]
        val form = formProvider(false).bind(Map("value" -> ""))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(form, Nil, operatorId, businessName)(request, messages(application)).toString
      }
    }
  }
}