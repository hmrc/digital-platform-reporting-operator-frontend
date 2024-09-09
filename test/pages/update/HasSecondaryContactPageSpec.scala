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

class HasSecondaryContactPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val emptyAnswers = UserAnswers("id")
    val operatorId = "operatorId"

  ".nextPage" - {

    "must go to Check Answers" - {

      "when the answer is no" in {

        val answers = emptyAnswers.set(HasSecondaryContactPage, false).success.value
        HasSecondaryContactPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }

      "when the answer is yes and Secondary Contact Name is answered" in {

        val answers =
          emptyAnswers
            .set(HasSecondaryContactPage, false).success.value
            .set(SecondaryContactNamePage, "name").success.value

        HasSecondaryContactPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }

    "must go to Secondary Contact Name when the answer is yes and Secondary Contact Name is not answered" in {

      val answers = emptyAnswers.set(HasSecondaryContactPage, true).success.value
      HasSecondaryContactPage.nextPage(operatorId, answers) mustEqual routes.SecondaryContactNameController.onPageLoad(operatorId)
    }
  }

  ".cleanup" - {

    "must remove secondary contact details when the answer is no" in {

      val answers =
        emptyAnswers
          .set(SecondaryContactNamePage, "name").success.value
          .set(SecondaryContactEmailPage, "email").success.value
          .set(CanPhoneSecondaryContactPage, true).success.value
          .set(SecondaryContactPhoneNumberPage, "phone").success.value

      val result = answers.set(HasSecondaryContactPage, false).success.value

      result.get(SecondaryContactNamePage)        must not be defined
      result.get(SecondaryContactEmailPage)       must not be defined
      result.get(CanPhoneSecondaryContactPage)    must not be defined
      result.get(SecondaryContactPhoneNumberPage) must not be defined
    }

    "must not remove secondary contact details when the answer is yes" in {

      val answers =
        emptyAnswers
          .set(SecondaryContactNamePage, "name").success.value
          .set(SecondaryContactEmailPage, "email").success.value
          .set(CanPhoneSecondaryContactPage, true).success.value
          .set(SecondaryContactPhoneNumberPage, "phone").success.value

      val result = answers.set(HasSecondaryContactPage, true).success.value

      result.get(SecondaryContactNamePage)        mustBe defined
      result.get(SecondaryContactEmailPage)       mustBe defined
      result.get(CanPhoneSecondaryContactPage)    mustBe defined
      result.get(SecondaryContactPhoneNumberPage) mustBe defined
    }
  }
}
