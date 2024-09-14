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

package forms

import forms.behaviours.CheckboxFieldBehaviours
import models.DueDiligence
import play.api.data.FormError

class DueDiligenceFormProviderSpec extends CheckboxFieldBehaviours {

  val businessName = "name"
  val form = new DueDiligenceFormProvider()(businessName)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "dueDiligence.error.required"

    behave like checkboxField[DueDiligence](
      form,
      fieldName,
      validValues  = DueDiligence.values,
      invalidError = FormError(s"$fieldName[0]", "error.invalid", Seq(businessName))
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey,
      Seq(businessName)
    )

    behave like checkboxFieldWithMutuallyExclusiveAnswers[DueDiligence](
      form,
      fieldName,
      DueDiligence.activeValues,
      Set(DueDiligence.NoDueDiligence),
      FormError(fieldName, "dueDiligence.error.mutuallyExclusive", args = Seq(businessName))
    )
  }
}
