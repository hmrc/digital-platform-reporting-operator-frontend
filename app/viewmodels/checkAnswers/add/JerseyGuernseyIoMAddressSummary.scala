/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.checkAnswers.add

import controllers.add.routes
import models.{CheckMode, UserAnswers}
import pages.add.{BusinessNamePage, JerseyGuernseyIoMAddressPage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object JerseyGuernseyIoMAddressSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    for {
      answer       <- answers.get(JerseyGuernseyIoMAddressPage)
      businessName <- answers.get(BusinessNamePage)
    } yield {

      val value = Seq(
        Some(HtmlFormat.escape(answer.line1)),
        answer.line2.map(HtmlFormat.escape),
        Some(HtmlFormat.escape(answer.town)),
        answer.county.map(HtmlFormat.escape),
        Some(HtmlFormat.escape(answer.postCode)),
        Some(HtmlFormat.escape(answer.country.name))
      ).flatten.map(_.toString).mkString("<br/>")

      SummaryListRowViewModel(
        key     = "jerseyGuernseyIoMAddress.checkYourAnswersLabel",
        value   = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", routes.JerseyGuernseyIoMAddressController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("JerseyGuernseyIoMAddress.change.hidden", businessName))
        )
      )
    }
  }
}
