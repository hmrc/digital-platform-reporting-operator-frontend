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

import forms.behaviours.FieldBehaviours
import models.DueDiligence
import play.api.data.FormError

class DueDiligenceActiveOnlyFormProviderSpec extends FieldBehaviours {

  private val businessName = "name"
  private val form = new DueDiligenceActiveOnlyFormProvider()(businessName)

  ".value" - {

    val fieldName = "value"

    "must bind true to Active Seller" in {

      val result = form.bind(Map(fieldName -> "true"))
      result.get mustEqual Set(DueDiligence.ActiveSeller)
      result.errors mustBe empty
    }

    "must bind false to No Due Diligence" in {

      val result = form.bind(Map(fieldName -> "false"))
      result.get mustEqual Set(DueDiligence.NoDueDiligence)
      result.errors mustBe empty
    }

    "must unbind a set containing Active Seller to true" in {
      val set = Set[DueDiligence](DueDiligence.ActiveSeller)
      val filledForm = form.fill(set)
      filledForm(fieldName).value.value mustEqual "true"
      filledForm.hasErrors mustEqual false
    }

    "must unbind a set not containing Active Seller to false" in {
      val set = Set[DueDiligence](DueDiligence.NoDueDiligence)
      val filledForm = form.fill(set)
      filledForm(fieldName).value.value mustEqual "false"
      filledForm.hasErrors mustEqual false
    }

    "must not bind non-booleans" in {

      forAll(nonBooleans) { nonBoolean =>
        val result = form.bind(Map(fieldName -> nonBoolean)).apply(fieldName)
        result.errors mustBe Seq(FormError(fieldName, "error.boolean", Seq(businessName)))
      }
    }

    "must not bind when key is not present at all" in {

      val result = form.bind(emptyForm).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, "dueDiligence.activeOnly.error.required", Seq(businessName)))
    }

    "not bind blank values" in {

      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, "dueDiligence.activeOnly.error.required", Seq(businessName)))
    }
  }
}
