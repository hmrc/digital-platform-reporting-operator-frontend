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
import controllers.{routes => baseRoutes}
import forms.UkAddressFormProvider
import models.{Country, DefaultCountriesList, NormalMode, RegisteredAddressCountry, UkAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.add.{BusinessNamePage, UkAddressPage}
import pages.update.RegisteredInUkPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.add.UkAddressView

import scala.concurrent.Future

class UkAddressControllerSpec extends SpecBase with MockitoSugar {

  private val countriesList = new DefaultCountriesList
  private val formProvider = new UkAddressFormProvider()
  private val businessName = "name"
  private val form = formProvider()
  private val baseAnswers = emptyUserAnswers.set(BusinessNamePage, businessName).success.value
  private val gbCountry: Country = Country("GB", "United Kingdom")

  private lazy val ukAddressRoute = routes.UkAddressController.onPageLoad(NormalMode).url

  private val validAnswer = UkAddress("line 1", None, "town", None, "AA1 1AA", Country("GB", "United Kingdom"))
  private val userAnswers = baseAnswers.set(UkAddressPage, validAnswer).success.value

  "UkAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, ukAddressRoute)

        val view = application.injector.instanceOf[UkAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, businessName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered as Uk address" in {

      val updatedAnswers = userAnswers.set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, ukAddressRoute)

        val view = application.injector.instanceOf[UkAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(UkAddress("line 1", None, "town", None, "AA1 1AA",
          Country("GB", "United Kingdom"))), NormalMode, businessName)(request, messages(application)).toString
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
          FakeRequest(POST, ukAddressRoute)
            .withFormUrlEncodedBody(("line1", "line 1"), ("town", "town"), ("postCode", "AA1 1AA"), ("country", gbCountry.code))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual UkAddressPage.nextPage(NormalMode, emptyUserAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val updatedAnswers = baseAnswers.set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, ukAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[UkAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, businessName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, ukAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, ukAddressRoute).withFormUrlEncodedBody(
          ("line1", "line 1"),
          ("town", "town 2"),
          ("postCode", "AA1 1AA"),
          ("country", countriesList.crownDependantCountries.head.code)
        )
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
