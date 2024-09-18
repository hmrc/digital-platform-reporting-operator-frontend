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

import controllers.{routes => baseRoutes}
import controllers.notification.{routes => notificationRoutes}
import models.{NormalMode, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class PlatformOperatorAddedPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")

    "must go to notifications Start page when the answer is yes" - {

      val answers = emptyAnswers.set(PlatformOperatorAddedPage, true).success.value
      PlatformOperatorAddedPage.nextPage(NormalMode, answers) mustEqual notificationRoutes.StartController.onPageLoad
    }

    "must go to platform operators when the answer is no" - {

      val answers = emptyAnswers.set(PlatformOperatorAddedPage, false).success.value
      PlatformOperatorAddedPage.nextPage(NormalMode, answers) mustEqual baseRoutes.PlatformOperatorsController.onPageLoad
    }
  }
}
