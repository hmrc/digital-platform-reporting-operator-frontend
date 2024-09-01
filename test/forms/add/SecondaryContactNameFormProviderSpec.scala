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

package forms.add

import forms.Validation
import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class SecondaryContactNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "secondaryContactName.error.required"
  val lengthKey = "secondaryContactName.error.length"
  val formatKey = "secondaryContactName.error.format"
  val maxLength = 105

  val businessName = "name"
  val form = new SecondaryContactNameFormProvider()(businessName)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      safeTextInputsWithMaxLength(maxLength)
    )

    behave like fieldThatDoesNotBindInvalidData(
      form,
      fieldName,
      unsafeTextInputsWithMaxLength(maxLength),
      FormError(fieldName, formatKey, Seq(Validation.textInputPattern.toString))
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength, businessName))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(businessName))
    )
  }
}
