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
import models.{CheckMode, NormalMode, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class SecondaryContactEmailPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")

    "in Normal Mode" - {

      "must go to Can Phone Secondary Contact" in {

        SecondaryContactEmailPage.nextPage(NormalMode, emptyAnswers) mustEqual routes.CanPhoneSecondaryContactController.onPageLoad(NormalMode)
      }
    }

    "in Check Mode" - {

      "must go to Check Answers when Can Phone Secondary Contact has been answered" in {

        val answers = emptyAnswers.set(CanPhonePrimaryContactPage, false).success.value
        SecondaryContactEmailPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
      }

      "must go to Can Phone Secondary Contact when that has not been answered" in {

        SecondaryContactEmailPage.nextPage(CheckMode, emptyAnswers) mustEqual routes.CanPhoneSecondaryContactController.onPageLoad(CheckMode)
      }
    }
  }
}