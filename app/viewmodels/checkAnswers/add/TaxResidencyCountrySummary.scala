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

package viewmodels.checkAnswers.add

import controllers.add.routes
import models.{CheckMode, UserAnswers}
import pages.add.{BusinessNamePage, TaxResidencyCountryPage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TaxResidencyCountrySummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      answer       <- answers.get(TaxResidencyCountryPage)
      businessName <- answers.get(BusinessNamePage)
    } yield {

        SummaryListRowViewModel(
          key     = messages("taxResidencyCountry.checkYourAnswersLabel", businessName),
          value   = ValueViewModel(HtmlFormat.escape(answer.name).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TaxResidencyCountryController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("taxResidencyCountry.change.hidden", businessName))
          )
        )
    }
}
