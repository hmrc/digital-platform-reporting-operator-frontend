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
import models.{CheckMode, NormalMode, NotificationType, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class ReportingPeriodPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val operatorId = "operatorId"
  private val emptyAnswers = UserAnswers("id")

  ".nextPage" - {

    "in Normal Mode" - {

      "must go to Due Diligence when Notification Type is RPO" in {

        val answers = emptyAnswers.set(NotificationTypePage, NotificationType.Rpo).success.value
        ReportingPeriodPage.nextPage(NormalMode, operatorId, answers) mustEqual routes.DueDiligenceController.onPageLoad(NormalMode, operatorId)
      }

      "must go to Check Answers when Notification Type is EPO" in {

        val answers = emptyAnswers.set(NotificationTypePage, NotificationType.Epo).success.value
        ReportingPeriodPage.nextPage(NormalMode, operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }

    "in Check Mode" - {

      "must go to Check Answers" in {

        ReportingPeriodPage.nextPage(CheckMode, operatorId, emptyAnswers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }
  }
}
