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
import org.scalacheck.Gen
import play.api.data.FormError

class SecondaryContactEmailFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "secondaryContactEmail.error.required"
  val lengthKey = "secondaryContactEmail.error.length"
  val formatKey = "secondaryContactEmail.error.format"
  val maxLength = 132

  val contactName = "name"
  val form = new SecondaryContactEmailFormProvider()(contactName)

  private val basicEmail = Gen.const("foo@example.com")
  private val emailWithSpecialChars = Gen.const("!#$%&'*-+/=?^_`{}~123@foo-bar.example.com")
  private val validData = Gen.oneOf(basicEmail, emailWithSpecialChars)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength, contactName))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(contactName))
    )

    "must not allow invalid email addresses" in {

      val noAt = "fooexample.com"
      val noUserName = "@example.com"
      val noDomain = "foo@example"
      val invalidData = Gen.oneOf(noAt, noUserName, noDomain).sample.value

      val result = form.bind(Map("value" -> invalidData)).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, formatKey, Seq(Validation.emailPattern.toString)))
    }
  }
}
