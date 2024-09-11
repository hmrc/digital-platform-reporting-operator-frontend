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
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class SecondaryContactEmailPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")
    val operatorId = "operatorId"

    "must go to Check Answers when Can Phone Secondary Contact has been answered" in {

      val answers = emptyAnswers.set(CanPhoneSecondaryContactPage, true).success.value
      SecondaryContactEmailPage.nextPage(operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
    }

    "must go to Can Phone Secondary Contact when it has not been answered" in {

      SecondaryContactEmailPage.nextPage(operatorId, emptyAnswers) mustEqual routes.CanPhoneSecondaryContactController.onPageLoad(operatorId)
    }
  }
}