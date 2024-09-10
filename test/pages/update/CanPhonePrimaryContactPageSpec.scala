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
import models.{CheckMode, NormalMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class CanPhonePrimaryContactPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val emptyAnswers = UserAnswers("id")
  private val operatorId = "operatorId"

  ".nextPage" - {

    "must go to Check Answers" - {

      "when the answer is no" in {

        val answers = emptyAnswers.set(CanPhonePrimaryContactPage, false).success.value
        CanPhonePrimaryContactPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }

      "when the answer is yes and Primary Contact Phone Number is answered" in {

        val answers =
          emptyAnswers
            .set(CanPhonePrimaryContactPage, true).success.value
            .set(PrimaryContactPhoneNumberPage, "phone").success.value

        CanPhonePrimaryContactPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }

    "must go to Primary Contact Phone Number when the answer is yes and Primary Contact Phone Number is not answered" in {

      val answers = emptyAnswers.set(CanPhonePrimaryContactPage, true).success.value
      CanPhonePrimaryContactPage.nextPage(operatorId, answers) mustEqual routes.PrimaryContactPhoneNumberController.onPageLoad(operatorId)
    }
  }

  ".cleanup" - {

    "must remove Primary Contact Phone Number when the answer is no" in {

      val answers = emptyAnswers.set(PrimaryContactPhoneNumberPage, "phone").success.value

      val result = answers.set(CanPhonePrimaryContactPage, false).success.value

      result.get(PrimaryContactPhoneNumberPage) must not be defined
    }

    "must not remove Primary Contact Phone Number when the answer is yes" in {

      val answers = emptyAnswers.set(PrimaryContactPhoneNumberPage, "phone").success.value

      val result = answers.set(CanPhonePrimaryContactPage, true).success.value

      result.get(PrimaryContactPhoneNumberPage) mustBe defined
    }
  }
}