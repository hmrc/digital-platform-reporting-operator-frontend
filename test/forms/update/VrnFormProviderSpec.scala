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

package forms.update

import forms.Validation
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.data.FormError

class VrnFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "vrn.error.required"
  val formatKey = "vrn.error.format"

  val businessName = "name"
  val form = new VrnFormProvider()(businessName)

  ".value" - {

    val fieldName = "value"

    val validData = for {
      prefix <- Gen.option(Gen.const("GB"))
      digits <- Gen.listOfN(9, Gen.numChar)
    } yield prefix.getOrElse("") + digits.mkString

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

    "must not bind invalid values" in {

      forAll(arbitrary[String]) { input =>

        whenever(input.trim.nonEmpty && !input.trim.matches(Validation.vrnPattern.toString)) {
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.errors must contain only FormError(fieldName, formatKey, Seq(Validation.vrnPattern.toString, businessName))
        }
      }
    }


    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(businessName))
    )
  }
}
