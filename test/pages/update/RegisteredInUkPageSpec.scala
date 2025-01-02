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

package pages.update

import controllers.update.routes
import models.{Country, DefaultCountriesList, InternationalAddress, JerseyGuernseyIoMAddress, RegisteredAddressCountry, UkAddress, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.add.UkAddressPage

class RegisteredInUkPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val countriesList = new DefaultCountriesList
  private val emptyAnswers = UserAnswers("id")
  private val operatorId = "operatorId"

  ".nextPage" - {

    "must go to Check Answers" - {

      "when the answer is Uk and UK Address has been answered" in {

        val answers =
          emptyAnswers
            .set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country("GB", "United Kingdom"))).success.value

        RegisteredInUkPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }

      "when the answer is International and International Address has been answered" in {

        val answers =
          emptyAnswers
            .set(RegisteredInUkPage, RegisteredAddressCountry.International).success.value
            .set(InternationalAddressPage, InternationalAddress("line 1", None, "city", None, "zip", countriesList.internationalCountries.head)).success.value

        RegisteredInUkPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }

      "when the answer is JerseyGeurnseyIoM and JerseyGeurnseyIoM Address has been answered" in {

        val answers =
          emptyAnswers
            .set(RegisteredInUkPage, RegisteredAddressCountry.JerseyGuernseyIsleOfMan).success.value
            .set(JerseyGuernseyIoMAddressPage, JerseyGuernseyIoMAddress("line 1", None, "city", None, "zip", Country("JE", "Jersey"))).success.value

        RegisteredInUkPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }

    "must go to UK Address when the answer is Uk and UK Address has not been answered" in {

      val answers = emptyAnswers.set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
      RegisteredInUkPage.nextPage(operatorId, answers) mustEqual routes.UkAddressController.onPageLoad(operatorId)
    }

    "must go to International Address when the answer is International and International Address has not been answered" in {

      val answers = emptyAnswers.set(RegisteredInUkPage, RegisteredAddressCountry.International).success.value
      RegisteredInUkPage.nextPage(operatorId, answers) mustEqual routes.InternationalAddressController.onPageLoad(operatorId)
    }

    "must go to JerseyGeurnseyIoM Address when the answer is JerseyGeurnseyIoM and JerseyGeurnseyIoM Address has not been answered" in {

      val answers = emptyAnswers.set(RegisteredInUkPage, RegisteredAddressCountry.JerseyGuernseyIsleOfMan).success.value
      RegisteredInUkPage.nextPage(operatorId, answers) mustEqual routes.JerseyGuernseyIoMAddressController.onPageLoad(operatorId)
    }
  }

  ".cleanup" - {

    "must remove UK & JerseyGeurnseyIoM address when the answer is International" in {

      val answers =
        emptyAnswers
          .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country("GB", "United Kingdom"))).success.value
          .set(JerseyGuernseyIoMAddressPage, JerseyGuernseyIoMAddress("line 1", None, "town", None, "AA1 1AA", Country("IM", "The Isle of Man"))).success.value
          .set(InternationalAddressPage, InternationalAddress("line 1", None, "city", None, "zip", countriesList.internationalCountries.head)).success.value

      val result = answers.set(RegisteredInUkPage, RegisteredAddressCountry.International).success.value

      result.get(UkAddressPage) must not be defined
      result.get(JerseyGuernseyIoMAddressPage) must not be defined
      result.get(InternationalAddressPage) mustBe defined
    }

    "must remove International & JerseyGuernseyIoM address when the answer is Uk" in {

      val answers =
        emptyAnswers
          .set(JerseyGuernseyIoMAddressPage, JerseyGuernseyIoMAddress("line 1", None, "town", None, "AA1 1AA", Country("GG", "Guernsey"))).success.value
          .set(InternationalAddressPage, InternationalAddress("line 1", None, "city", None, "zip", countriesList.internationalCountries.head)).success.value

      val result = answers.set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
        .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country("GB", "United Kingdom"))).success.value

      result.get(UkAddressPage) mustBe defined
      result.get(JerseyGuernseyIoMAddressPage) must not be defined
      result.get(InternationalAddressPage) must not be defined
    }

    "must remove Uk and International address when the answer is JerseyGuernseyIoM" in {

      val answers =
        emptyAnswers
          .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country("GB", "United Kingdom"))).success.value
          .set(JerseyGuernseyIoMAddressPage, JerseyGuernseyIoMAddress("line 1", None, "town", None, "AA1 1AA", Country("JE", "Jersey"))).success.value
          .set(InternationalAddressPage, InternationalAddress("line 1", None, "city", None, "zip", countriesList.internationalCountries.head)).success.value

      val result = answers.set(RegisteredInUkPage, RegisteredAddressCountry.JerseyGuernseyIsleOfMan).success.value

      result.get(UkAddressPage) must not be defined
      result.get(JerseyGuernseyIoMAddressPage) mustBe defined
      result.get(InternationalAddressPage) must not be defined
    }
  }
}