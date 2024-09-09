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
import models.{BusinessType, CheckMode, NormalMode, UkTaxIdentifiers, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class HasUkTaxIdentifierPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val emptyAnswers = UserAnswers("id")
    val operatorId = "operatorId"

  ".nextPage" - {

    "must go to Check Answers" - {

      "when the answer is no" in {

        val answers = emptyAnswers.set(HasUkTaxIdentifierPage, false).success.value
        HasUkTaxIdentifierPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }

      "when the answer is yes and UK Tax Identifier has been answered" in {

        val answers =
          emptyAnswers
            .set(HasUkTaxIdentifierPage, true).success.value
            .set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](UkTaxIdentifiers.Chrn)).success.value

        HasUkTaxIdentifierPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }

    "must go to UK Tax Identifiers when the answer is yes and UK Tax Identifiers has not been answered" in {

      val answers = emptyAnswers.set(HasUkTaxIdentifierPage, true).success.value
      HasUkTaxIdentifierPage.nextPage(operatorId, answers) mustEqual routes.UkTaxIdentifiersController.onPageLoad(operatorId)
    }
  }

  ".cleanup" - {

    "must remove UK tax identifier details when the answer is no" in {

      val answers =
        emptyAnswers
          .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
          .set(BusinessTypePage, BusinessType.LimitedCompany).success.value
          .set(UtrPage, "utr").success.value
          .set(CrnPage, "crn").success.value
          .set(VrnPage, "vrn").success.value
          .set(EmprefPage, "empref").success.value
          .set(ChrnPage, "chrn").success.value

      val result = answers.set(HasUkTaxIdentifierPage, false).success.value

      result.get(UkTaxIdentifiersPage) must not be defined
      result.get(BusinessTypePage)     must not be defined
      result.get(UtrPage)              must not be defined
      result.get(CrnPage)              must not be defined
      result.get(VrnPage)              must not be defined
      result.get(EmprefPage)           must not be defined
      result.get(ChrnPage)             must not be defined
    }

    "must not remove UK tax identifier details when the answer is yes" in {

      val answers =
        emptyAnswers
          .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
          .set(BusinessTypePage, BusinessType.LimitedCompany).success.value
          .set(UtrPage, "utr").success.value
          .set(CrnPage, "crn").success.value
          .set(VrnPage, "vrn").success.value
          .set(EmprefPage, "empref").success.value
          .set(ChrnPage, "chrn").success.value

      val result = answers.set(HasUkTaxIdentifierPage, true).success.value

      result.get(UkTaxIdentifiersPage) mustBe defined
      result.get(BusinessTypePage)     mustBe defined
      result.get(UtrPage)              mustBe defined
      result.get(CrnPage)              mustBe defined
      result.get(VrnPage)              mustBe defined
      result.get(EmprefPage)           mustBe defined
      result.get(ChrnPage)             mustBe defined
    }
  }
}