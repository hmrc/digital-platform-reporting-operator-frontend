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
import models.{Country, UkTaxIdentifiers, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class HasTaxIdentifierPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val emptyAnswers = UserAnswers("id")
  private val operatorId = "operatorId"

  ".nextPage" - {

    "must go to Check Answers" - {

      "when the answer is no" in {

        val answers = emptyAnswers.set(HasTaxIdentifierPage, false).success.value
        HasTaxIdentifierPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }

      "when the answer is yes and Tax Resident in UK has been answered" in {

        val answers =
          emptyAnswers
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, true).success.value

        HasTaxIdentifierPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }

    "must go to Tax Resident in UK when the answer is yes and Tax Resident in UK has not been answered" in {

      val answers = emptyAnswers.set(HasTaxIdentifierPage, true).success.value
      HasTaxIdentifierPage.nextPage(operatorId, answers) mustEqual routes.TaxResidentInUkController.onPageLoad(operatorId)
    }
  }

  ".cleanup" - {

    "must remove tax identifier details when the answer is no" in {

      val answers =
        emptyAnswers
          .set(TaxResidentInUkPage, true).success.value
          .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
          .set(UtrPage, "utr").success.value
          .set(CrnPage, "crn").success.value
          .set(VrnPage, "vrn").success.value
          .set(EmprefPage, "empref").success.value
          .set(ChrnPage, "chrn").success.value
          .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
          .set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(HasTaxIdentifierPage, false).success.value

      result.get(TaxResidentInUkPage)            must not be defined
      result.get(UkTaxIdentifiersPage)           must not be defined
      result.get(UtrPage)                        must not be defined
      result.get(CrnPage)                        must not be defined
      result.get(VrnPage)                        must not be defined
      result.get(EmprefPage)                     must not be defined
      result.get(ChrnPage)                       must not be defined
      result.get(TaxResidencyCountryPage)        must not be defined
      result.get(InternationalTaxIdentifierPage) must not be defined
    }

    "must not remove UK tax identifier details when the answer is yes" in {

      val answers =
        emptyAnswers
          .set(TaxResidentInUkPage, true).success.value
          .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
          .set(UtrPage, "utr").success.value
          .set(CrnPage, "crn").success.value
          .set(VrnPage, "vrn").success.value
          .set(EmprefPage, "empref").success.value
          .set(ChrnPage, "chrn").success.value
          .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
          .set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(HasTaxIdentifierPage, true).success.value

      result.get(TaxResidentInUkPage)            mustBe defined
      result.get(UkTaxIdentifiersPage)           mustBe defined
      result.get(UtrPage)                        mustBe defined
      result.get(CrnPage)                        mustBe defined
      result.get(VrnPage)                        mustBe defined
      result.get(EmprefPage)                     mustBe defined
      result.get(ChrnPage)                       mustBe defined
      result.get(TaxResidencyCountryPage)        mustBe defined
      result.get(InternationalTaxIdentifierPage) mustBe defined
    }
  }
}