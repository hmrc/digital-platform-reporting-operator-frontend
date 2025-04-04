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

import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.data.FormError

class CrnFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "crn.error.required"
  val formatKey = "crn.error.format"

  val businessName = "name"
  val form = new CrnFormProvider()(businessName)
  val fieldName = "value"

  "Eight digits Crn" - {
    val eightDigitsCrn = for {
      digits <- Gen.listOfN(8, Gen.numChar)
    } yield digits.mkString

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      eightDigitsCrn
    )
  }

  "One letter Seven number Crn" - {
    val oneLetterSevenNumbersCrn = for {
      first <- Gen.listOfN(1, Gen.alphaUpperChar).map(_.mkString)
      digits <- Gen.listOfN(7, Gen.numChar)
    } yield first + digits.mkString

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      oneLetterSevenNumbersCrn
    )
  }

  "Two letter Six number Crn" - {
    val twoLettersSixNumbersCrn = for {
      first <- Gen.listOfN(2, Gen.alphaUpperChar).map(_.mkString)
      digits <- Gen.listOfN(6, Gen.numChar)
    } yield first + digits.mkString

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      twoLettersSixNumbersCrn
    )
  }

  "must not bind invalid values" in {
    forAll(arbitrary[String]) { input =>
      whenever(input.trim.nonEmpty && !input.trim.matches(Validation.crnPattern.toString)) {
        val result = form.bind(Map(fieldName -> input)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatKey, Seq(Validation.crnPattern.toString, businessName))
      }
    }
  }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(businessName))
    )
  }
