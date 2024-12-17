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
import models.{CheckMode, NormalMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class HasTradingNamePageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val emptyAnswers = UserAnswers("id")

  ".nextPage" - {

    "in Normal Mode" - {

      "must go to Trading Name when the answer is yes" in {

        val answers = emptyAnswers.set(HasTradingNamePage, true).success.value
        HasTradingNamePage.nextPage(NormalMode, answers) mustEqual routes.TradingNameController.onPageLoad(NormalMode)
      }

      "must go to Uk Tax Identifiers when the answer is no" in {

        val answers = emptyAnswers.set(HasTradingNamePage, false).success.value
        HasTradingNamePage.nextPage(NormalMode, answers) mustEqual routes.UkTaxIdentifiersController.onPageLoad(NormalMode)
      }
    }

    "in Check Mode" - {

      "must go to Check Answers" - {

        "when the answer is no" in {

          val answers = emptyAnswers.set(HasTradingNamePage, false).success.value
          HasTradingNamePage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
        }

        "when the answer is yes and Trading Name is already answered" in {

          val answers =
            emptyAnswers
              .set(HasTradingNamePage, true).success.value
              .set(TradingNamePage, "name").success.value

          HasTradingNamePage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go to Trading Name when the answer is yes and Trading Name has not been answered" in {

        val answers = emptyAnswers.set(HasTradingNamePage, true).success.value
        HasTradingNamePage.nextPage(CheckMode, answers) mustEqual routes.TradingNameController.onPageLoad(CheckMode)
      }
    }
  }

  ".cleanup" - {

    "must remove Trading Name when the answer is no" in {

      val answers = emptyAnswers.set(TradingNamePage, "name").success.value

      val result = answers.set(HasTradingNamePage, false).success.value

      result.get(TradingNamePage) must not be defined
    }

    "must not remove Trading Name when the answer is yes" in {

      val answers = emptyAnswers.set(TradingNamePage, "name").success.value

      val result = answers.set(HasTradingNamePage, true).success.value

      result.get(TradingNamePage) mustBe defined
    }
  }
}