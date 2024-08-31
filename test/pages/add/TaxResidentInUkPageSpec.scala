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
import models.{CheckMode, Country, NormalMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class TaxResidentInUkPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")

    "in Normal Mode" - {

      "must go to Has UK Tax Identifier when the answer is yes" in {

        val answers = emptyAnswers.set(TaxResidentInUkPage, true).success.value
        TaxResidentInUkPage.nextPage(NormalMode, answers) mustEqual routes.HasUkTaxIdentifierController.onPageLoad(NormalMode)
      }

      "must go to Tax Residency Country when the answer is no" in {

        val answers = emptyAnswers.set(TaxResidentInUkPage, false).success.value
        TaxResidentInUkPage.nextPage(NormalMode, answers) mustEqual routes.TaxResidencyCountryController.onPageLoad(NormalMode)
      }
    }

    "in Check Mode" - {

      "must go to Check Answers" - {

        "when the answer is yes and Has UK Tax Identifier has been answered" in {

          val answers =
            emptyAnswers
              .set(TaxResidentInUkPage, true).success.value
              .set(HasUkTaxIdentifierPage, true).success.value

          TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
        }

        "when the answer is no and Tax Residency Country has been answered" in {

          val answers =
            emptyAnswers
              .set(TaxResidentInUkPage, false).success.value
              .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value

          TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go to Has UK Tax Identifier when the answer is yes and Has UK Tax Identifier has not been answered" in {

        val answers = emptyAnswers.set(TaxResidentInUkPage, true).success.value
        TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.HasUkTaxIdentifierController.onPageLoad(CheckMode)
      }

      "must go to Tax Residency Country when the answer is no and Tax Residency Country has not been answered" in {

        val answers = emptyAnswers.set(TaxResidentInUkPage, false).success.value
        TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.TaxResidencyCountryController.onPageLoad(CheckMode)
      }
    }
  }
}