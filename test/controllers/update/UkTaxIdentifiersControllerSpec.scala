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
import controllers.{routes => baseRoutes}
import forms.UkTaxIdentifiersFormProvider
import models.UkTaxIdentifiers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.update.{BusinessNamePage, UkTaxIdentifiersPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.update.UkTaxIdentifiersView

import scala.concurrent.Future

class UkTaxIdentifiersControllerSpec extends SpecBase with MockitoSugar {

  private lazy val ukTaxIdentifiersRoute = routes.UkTaxIdentifiersController.onPageLoad(operatorId).url

  private val formProvider = new UkTaxIdentifiersFormProvider()
  private val businessName = "name"
  private val form = formProvider()
  private val baseAnswers = emptyUserAnswers.set(BusinessNamePage, businessName).success.value

  "UkTaxIdentifiers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, ukTaxIdentifiersRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UkTaxIdentifiersView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, operatorId, businessName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, ukTaxIdentifiersRoute)

        val view = application.injector.instanceOf[UkTaxIdentifiersView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(UkTaxIdentifiers.values.toSet), operatorId, businessName)(request, messages(application)).toString
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
          FakeRequest(POST, ukTaxIdentifiersRoute)
            .withFormUrlEncodedBody(("value[0]", UkTaxIdentifiers.values.head.toString))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](UkTaxIdentifiers.values.head)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual UkTaxIdentifiersPage.nextPage(operatorId, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, ukTaxIdentifiersRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[UkTaxIdentifiersView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, operatorId, businessName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, ukTaxIdentifiersRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, ukTaxIdentifiersRoute)
            .withFormUrlEncodedBody(("value[0]", UkTaxIdentifiers.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
