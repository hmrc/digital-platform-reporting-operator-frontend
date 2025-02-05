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
import builders.UkAddressBuilder.aUkAddress
import builders.UserAnswersBuilder.{aUserAnswers, anEmptyUserAnswer}
import models.RegisteredAddressCountry.Uk
import models.UkTaxIdentifiers
import pages.update._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.update.MissingInformationView

class MissingInformationControllerSpec extends SpecBase {

  "MissingInformation Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(aUserAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, routes.MissingInformationController.onPageLoad(aUserAnswers.operatorId.get).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[MissingInformationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(aUserAnswers.operatorId.get)(request, messages(application)).toString
      }
    }

    "must redirect to CheckYourAnswersPage when UpdatePlatformOperatorRequest successfully created" in {
      val names: Set[UkTaxIdentifiers] = Set(UkTaxIdentifiers.Utr)
      val userAnswers = anEmptyUserAnswer
        .set(BusinessNamePage, "some-business-name").success.value
        .set(HasTradingNamePage, true).success.value
        .set(TradingNamePage, "some-trading-name").success.value
        .set(UkTaxIdentifiersPage, names).success.value
        .set(UtrPage, "some-utr").success.value
        .set(PrimaryContactNamePage, "contact-name").success.value
        .set(PrimaryContactEmailPage, "contact-email@example.com").success.value
        .set(CanPhonePrimaryContactPage, true).success.value
        .set(PrimaryContactPhoneNumberPage, "some-phone-number").success.value
        .set(HasSecondaryContactPage, true).success.value
        .set(SecondaryContactNamePage, "secondary-contact-name").success.value
        .set(SecondaryContactEmailPage, "secondary-contact-name@example.com").success.value
        .set(CanPhoneSecondaryContactPage, true).success.value
        .set(SecondaryContactPhoneNumberPage, "some-secondary-phone-number").success.value
        .set(RegisteredInUkPage, Uk).success.value
        .set(UkAddressPage, aUkAddress).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.MissingInformationController.onSubmit(aUserAnswers.operatorId.get).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(aUserAnswers.operatorId.get).url
      }
    }

    "must redirect to the first controller for which data is missing" in {
      val names: Set[UkTaxIdentifiers] = Set(UkTaxIdentifiers.Utr)
      val userAnswers = anEmptyUserAnswer
        .set(BusinessNamePage, "some-business-name").success.value
        .set(HasTradingNamePage, true).success.value
        .set(TradingNamePage, "some-trading-name").success.value
        .set(UkTaxIdentifiersPage, names).success.value
        .set(UtrPage, "some-utr").success.value
        .set(PrimaryContactNamePage, "contact-name").success.value
        .set(PrimaryContactEmailPage, "contact-email@example.com").success.value
        .set(CanPhonePrimaryContactPage, true).success.value
        .set(PrimaryContactPhoneNumberPage, "some-phone-number").success.value
        .set(HasSecondaryContactPage, true).success.value
        .set(SecondaryContactNamePage, "secondary-contact-name").success.value
        .set(SecondaryContactEmailPage, "secondary-contact-name@example.com").success.value
        .set(CanPhoneSecondaryContactPage, true).success.value
        .set(SecondaryContactPhoneNumberPage, "some-secondary-phone-number").success.value
        .set(RegisteredInUkPage, Uk).success.value
        .set(UkAddressPage, aUkAddress).success.value
        .remove(UtrPage).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.MissingInformationController.onSubmit(aUserAnswers.operatorId.get).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UtrController.onPageLoad(aUserAnswers.operatorId.get).url
      }
    }
  }
}
