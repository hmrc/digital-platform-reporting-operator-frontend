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

package pages.add

import controllers.add.routes
import controllers.{routes => baseRoutes}
import models.{CheckMode, Country, InternationalAddress, NormalMode, UkAddress, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class RegisteredInUkPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")

    "in Normal Mode" - {

      "must go to UK Address when the answer is yes" in {

        val answers = emptyAnswers.set(RegisteredInUkPage, true).success.value
        RegisteredInUkPage.nextPage(NormalMode, answers) mustEqual routes.UkAddressController.onPageLoad(NormalMode)
      }

      "must go to International Address when the answer is no" in {

        val answers = emptyAnswers.set(RegisteredInUkPage, false).success.value
        RegisteredInUkPage.nextPage(NormalMode, answers) mustEqual routes.InternationalAddressController.onPageLoad(NormalMode)
      }
    }

    "in Check Mode" - {

      "must go to Check Answers" - {

        "when the answer is yes and UK Address has been answered" in {

          val answers =
            emptyAnswers
              .set(RegisteredInUkPage, true).success.value
              .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value

          RegisteredInUkPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
        }

        "when the answer is no and International Address has been answered" in {

          val answers =
            emptyAnswers
              .set(RegisteredInUkPage, false).success.value
              .set(InternationalAddressPage, InternationalAddress("line 1", None, "city", None, None, Country.internationalCountries.head)).success.value

          RegisteredInUkPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go to UK Address when the answer is yes and UK Address has not been answered" in {

        val answers = emptyAnswers.set(RegisteredInUkPage, true).success.value
        RegisteredInUkPage.nextPage(CheckMode, answers) mustEqual routes.UkAddressController.onPageLoad(CheckMode)
      }

      "must go to International Address when the answer is no and International Address has not been answered" in {

        val answers = emptyAnswers.set(RegisteredInUkPage, false).success.value
        RegisteredInUkPage.nextPage(CheckMode, answers) mustEqual routes.InternationalAddressController.onPageLoad(CheckMode)
      }
    }
  }
}