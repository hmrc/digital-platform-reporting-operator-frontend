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

package viewmodels

import controllers.routes
import models.operator.responses.PlatformOperator
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value}
import viewmodels.govuk.summarylist._

final case class PlatformOperatorViewModel(operatorId: String, operatorName: String) {

  def summaryListRow(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key     = Key(Text(operatorName)),
      value   = Value(Text(operatorId)),
      actions = Seq(
        ActionItemViewModel(Text(messages("site.view")), routes.IndexController.onPageLoad().url)
          .withVisuallyHiddenText(messages("platformOperators.view.hidden", operatorName))
      )
    )
}

object PlatformOperatorViewModel {

  def apply(operator: PlatformOperator): PlatformOperatorViewModel =
    PlatformOperatorViewModel(operator.operatorId, operator.operatorName)
}
