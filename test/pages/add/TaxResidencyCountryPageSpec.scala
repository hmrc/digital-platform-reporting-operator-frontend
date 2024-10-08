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
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class TaxResidencyCountryPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")

    "in Normal Mode" - {

      "must go to International Tax Identifier" in {

        TaxResidencyCountryPage.nextPage(NormalMode, emptyAnswers) mustEqual routes.InternationalTaxIdentifierController.onPageLoad(NormalMode)
      }
    }

    "in Check Mode" - {

      "must go to Check Answers when International Tax identifier has been answered" in {

        val answers = emptyAnswers.set(InternationalTaxIdentifierPage, "id").success.value
        TaxResidencyCountryPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
      }

      "must go to International Tax identifier when it has not been answered" in {

        TaxResidencyCountryPage.nextPage(CheckMode, emptyAnswers) mustEqual routes.InternationalTaxIdentifierController.onPageLoad(CheckMode)
      }
    }
  }
}