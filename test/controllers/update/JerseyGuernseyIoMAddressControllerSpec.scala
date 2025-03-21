/*
 * Copyright 2025 HM Revenue & Customs
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
import forms.JerseyGuernseyIoMAddressFormProvider
import models.{Country, DefaultCountriesList, JerseyGuernseyIoMAddress, UkAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.add.UkAddressPage
import pages.update.{BusinessNamePage, JerseyGuernseyIoMAddressPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.update.JerseyGuernseyIoMAddressView

import scala.concurrent.Future

class JerseyGuernseyIoMAddressControllerSpec extends SpecBase with MockitoSugar {

  private val countriesList = new DefaultCountriesList
  private val formProvider = new JerseyGuernseyIoMAddressFormProvider(countriesList)
  private val businessName = "name"
  private val form = formProvider()
  private val baseAnswers = emptyUserAnswers.set(BusinessNamePage, businessName).success.value

  private lazy val jerseyGuernseyIoMAddressRoute = routes.JerseyGuernseyIoMAddressController.onPageLoad(operatorId).url

  private val validAnswer = JerseyGuernseyIoMAddress("line 1", None, "town", None, "AA1 1AA", Country("GG", "Guernsey"))
  private val userAnswers = baseAnswers.set(JerseyGuernseyIoMAddressPage, validAnswer).success.value

  "JerseyGuernseyIoMAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, jerseyGuernseyIoMAddressRoute)

        val view = application.injector.instanceOf[JerseyGuernseyIoMAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, operatorId, businessName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, jerseyGuernseyIoMAddressRoute)

        val view = application.injector.instanceOf[JerseyGuernseyIoMAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(JerseyGuernseyIoMAddress("line 1", None, "town", None,
          "AA1 1AA", Country("GG", "Guernsey"))), operatorId, businessName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered but answer saved under UkAddress " in {

      val updatedUserAnswers = baseAnswers.set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country("GG", "Guernsey"))).success.value
      val application = applicationBuilder(userAnswers = Some(updatedUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, jerseyGuernseyIoMAddressRoute)

        val view = application.injector.instanceOf[JerseyGuernseyIoMAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(JerseyGuernseyIoMAddress("line 1", None, "town", None,
          "AA1 1AA", Country("GG", "Guernsey"))), operatorId, businessName)(request, messages(application)).toString
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
          FakeRequest(POST, jerseyGuernseyIoMAddressRoute)
            .withFormUrlEncodedBody(("line1", "line 1"), ("town", "town"), ("postCode", "AA1 1AA"), ("country", "GG"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JerseyGuernseyIoMAddressPage.nextPage(operatorId, emptyUserAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, jerseyGuernseyIoMAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[JerseyGuernseyIoMAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, operatorId, businessName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, jerseyGuernseyIoMAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, jerseyGuernseyIoMAddressRoute)
            .withFormUrlEncodedBody(("ine1", "value 1"), ("line2", "value 2"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
