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

package pages.notification

import controllers.notification.routes
import models.{CheckMode, DueDiligence, NormalMode, NotificationType, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class NotificationTypePageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val operatorId = "operatorId"
  private val emptyAnswers = UserAnswers("id")

  ".nextPage" - {

    "in Normal Mode" - {

      "must go to Reporting Period" in {

        NotificationTypePage.nextPage(NormalMode, operatorId, emptyAnswers) mustEqual routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId)
      }
    }

    "in Check Mode" - {

      "must go to Due Diligence when the answer is RPO and Due Diligence has not been answered" in {

        val answers = emptyAnswers.set(NotificationTypePage, NotificationType.Rpo).success.value
        NotificationTypePage.nextPage(CheckMode, operatorId, answers) mustEqual routes.DueDiligenceController.onPageLoad(CheckMode, operatorId)
      }

      "must go to Check Answers" - {

        "when the answer is EPO" in {

          val answers = emptyAnswers.set(NotificationTypePage, NotificationType.Epo).success.value
          NotificationTypePage.nextPage(CheckMode, operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
        }

        "when the answer is RPO and Due Diligence has been answered" in {

          val answers =
            emptyAnswers
              .set(NotificationTypePage, NotificationType.Epo).success.value
              .set(DueDiligencePage, Set[DueDiligence](DueDiligence.Extended)).success.value

          NotificationTypePage.nextPage(CheckMode, operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
        }
      }
    }
  }

  ".cleanup" - {

    "must remove DueDiligence when the answer is EPO" in {

      val answers = emptyAnswers.set(DueDiligencePage, DueDiligence.activeValues).success.value
      val result = answers.set(NotificationTypePage, NotificationType.Epo).success.value

      result.get(DueDiligencePage) must not be defined
    }

    "must not remove DueDiligence when the answer is RPO" in {

      val answers = emptyAnswers.set(DueDiligencePage, DueDiligence.activeValues).success.value
      val result = answers.set(NotificationTypePage, NotificationType.Rpo).success.value

      result.get(DueDiligencePage) mustBe  defined
    }
  }
}
