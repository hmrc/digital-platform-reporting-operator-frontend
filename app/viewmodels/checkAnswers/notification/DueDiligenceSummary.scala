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

package viewmodels.checkAnswers.notification

import controllers.notification.routes
import models.{CheckMode, UserAnswers}
import pages.notification.DueDiligencePage
import pages.update.BusinessNamePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.NotificationDetailsQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object DueDiligenceSummary {

  def row(operatorId: String, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      dueDiligence <- answers.get(DueDiligencePage)
      businessName <- answers.get(BusinessNamePage)
    } yield {

      val value = ValueViewModel(
        HtmlContent(
          dueDiligence.map {
              item => HtmlFormat.escape(messages(s"dueDiligence.checkAnswers.$item")).toString
            }
            .mkString(",<br>")
        )
      )

      SummaryListRowViewModel(
        key = messages("dueDiligence.checkYourAnswersLabel", businessName),
        value = value,
        actions = Seq(
          ActionItemViewModel("site.change", routes.DueDiligenceController.onPageLoad(CheckMode, operatorId).url)
            .withVisuallyHiddenText(messages("dueDiligence.change.hidden", businessName))
        )
      )
    }

  def summaryRow(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(NotificationDetailsQuery).flatMap { notifications =>
      notifications.sortBy(_.receivedDateTime).reverse.headOption.map { notification =>

        val answer = if (notification.dueDiligence.isEmpty) {
          messages("viewNotifications.dueDiligence.notApplicable")
        } else {
          notification.dueDiligence
            .map(x => messages(s"viewNotifications.dueDiligence.${x.toString}"))
            .mkString(",<br>")
        }

        SummaryListRowViewModel(
          key = messages("notificationAdded.dueDiligence"),
          value = ValueViewModel(HtmlContent(answer)),
          actions = Nil
        )
      }
    }
}
