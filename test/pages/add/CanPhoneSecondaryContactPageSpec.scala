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

class CanPhoneSecondaryContactPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")

    "in Normal Mode" - {

      "must go to Secondary Contact Phone Number when the answer is yes" in {

        val answers = emptyAnswers.set(CanPhoneSecondaryContactPage, true).success.value
        CanPhoneSecondaryContactPage.nextPage(NormalMode, answers) mustEqual routes.SecondaryContactPhoneNumberController.onPageLoad(NormalMode)
      }

      "must go to Check Your Answers when the answer is no" in {

        val answers = emptyAnswers.set(CanPhoneSecondaryContactPage, false).success.value
        CanPhoneSecondaryContactPage.nextPage(NormalMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "in Check Mode" - {

      "must go to Check Answers" - {

        "when the answer is no" in {

          val answers = emptyAnswers.set(CanPhoneSecondaryContactPage, false).success.value
          CanPhoneSecondaryContactPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
        }

        "when the answer is yes and Secondary Contact Phone Number is answered" in {

          val answers =
            emptyAnswers
              .set(CanPhoneSecondaryContactPage, true).success.value
              .set(SecondaryContactPhoneNumberPage, "phone").success.value

          CanPhoneSecondaryContactPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go to Secondary Contact Phone Number when the answer is yes and Secondary Contact Phone Number is not answered" in {

        val answers = emptyAnswers.set(CanPhoneSecondaryContactPage, true).success.value
        CanPhoneSecondaryContactPage.nextPage(CheckMode, answers) mustEqual routes.SecondaryContactPhoneNumberController.onPageLoad(CheckMode)
      }
    }
  }
}
