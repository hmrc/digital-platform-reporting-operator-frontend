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

package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.govuk.all.HintViewModel
import viewmodels.govuk.checkbox._

sealed trait DueDiligence

object DueDiligence extends Enumerable.Implicits {

  case object Extended extends WithName("extendedDueDiligence") with DueDiligence
  case object ActiveSeller extends WithName("activeSellerDueDiligence") with DueDiligence
  case object NoDueDiligence extends WithName("noDueDiligence") with DueDiligence

  val values: Seq[DueDiligence] = Seq(
    Extended, ActiveSeller, NoDueDiligence
  )

  val activeValues: Set[DueDiligence] = Set(Extended, ActiveSeller)

  def checkboxItems(implicit messages: Messages): Seq[CheckboxItem] =
    Seq(
      CheckboxItemViewModel(
        content = Text(messages(s"dueDiligence.${ActiveSeller.toString}")),
        fieldId = "value",
        index = 0,
        value = ActiveSeller.toString
      ),
      CheckboxItemViewModel(
        content = Text(messages(s"dueDiligence.${Extended.toString}")),
        fieldId = "value",
        index = 1,
        value = Extended.toString
      ),
      CheckboxItem(divider = Some(messages("site.or"))),
      CheckboxItemViewModel(
        content = Text(messages(s"dueDiligence.${NoDueDiligence.toString}")),
        fieldId = "value",
        index = 2,
        value = NoDueDiligence.toString
      ).withHint(HintViewModel(Text(messages("dueDiligence.noDueDiligence.hint"))))
    )

  implicit val enumerable: Enumerable[DueDiligence] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
