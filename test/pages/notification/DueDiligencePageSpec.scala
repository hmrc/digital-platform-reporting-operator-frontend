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
import models.{CheckMode, DueDiligence, NormalMode, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class DueDiligencePageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val operatorId = "operatorId"
  private val emptyAnswers = UserAnswers("id")

  ".nextPage" - {

    "in Normal Mode" - {

      "must go to Reporting in First Period when the answer contains Extended DD" in {

        val answers = emptyAnswers.set(DueDiligencePage, Set[DueDiligence](DueDiligence.Extended)).success.value
        DueDiligencePage.nextPage(NormalMode, operatorId, answers) mustEqual routes.ReportingInFirstPeriodController.onPageLoad(NormalMode, operatorId)
      }

      "must go to Check Answers when the answer does not contain Extended DD" in {

        val answers = emptyAnswers.set(DueDiligencePage, Set[DueDiligence](DueDiligence.ActiveSeller)).success.value
        DueDiligencePage.nextPage(NormalMode, operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }

    "in Check Mode" - {

      "must go to Reporting in First Period when the answer contains Extended DD and Reporting in First Period has not been answered" in {

        val answers = emptyAnswers.set(DueDiligencePage, Set[DueDiligence](DueDiligence.Extended)).success.value
        DueDiligencePage.nextPage(CheckMode, operatorId, answers) mustEqual routes.ReportingInFirstPeriodController.onPageLoad(CheckMode, operatorId)
      }

      "must go to Check Answers" - {

        "when the answer contains Extended DD and Reporting in First Period has been answered" in {

          val answers =
            emptyAnswers
              .set(DueDiligencePage, Set[DueDiligence](DueDiligence.Extended)).success.value
              .set(ReportingInFirstPeriodPage, true).success.value

          DueDiligencePage.nextPage(CheckMode, operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
        }

        "when the answer does not contain Extended DD" in {

          val answers = emptyAnswers.set(DueDiligencePage, Set[DueDiligence](DueDiligence.ActiveSeller)).success.value
          DueDiligencePage.nextPage(CheckMode, operatorId, answers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
        }
      }
    }
  }

  ".cleanup" - {

    "must remove Reporting in First Period when the answer does not contain Extended DD" in {

      val answers = emptyAnswers.set(ReportingInFirstPeriodPage, true).success.value

      val result = answers.set(DueDiligencePage, Set[DueDiligence](DueDiligence.ActiveSeller)).success.value

      result.get(ReportingInFirstPeriodPage) must not be defined
    }

    "must not remove Reporting in First Period when the answer contains Extended DD" in {

      val answers = emptyAnswers.set(ReportingInFirstPeriodPage, true).success.value

      val result = answers.set(DueDiligencePage, Set[DueDiligence](DueDiligence.Extended)).success.value

      result.get(ReportingInFirstPeriodPage) mustBe defined
    }
  }
}
