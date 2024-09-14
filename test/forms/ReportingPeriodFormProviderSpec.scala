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

import forms.behaviours.IntFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import java.time.{Clock, LocalDate, ZoneOffset}

class ReportingPeriodFormProviderSpec extends IntFieldBehaviours {

  private val requiredKey = "reportingPeriod.error.required"

  private val businessName = "name"
  private val clock = Clock.systemDefaultZone.withZone(ZoneOffset.UTC)
  private val form = new ReportingPeriodFormProvider(clock)(businessName)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.choose(LocalDate.now(clock).getYear, LocalDate.now(clock).getYear + 1).map(_.toString)
    )

    behave like mandatoryField(
      form,
      fieldName,
      FormError(fieldName, requiredKey, Seq(businessName))
    )

    behave like intFieldWithMinimum(
      form,
      fieldName,
      2024,
      FormError(fieldName, "reportingPeriod.error.belowMinimum", Seq(2024))
    )

    behave like intFieldWithMaximum(
      form,
      fieldName,
      LocalDate.now(clock).getYear + 1,
      FormError(fieldName, "reportingPeriod.error.aboveMaximum", Seq(LocalDate.now(clock).getYear + 1))
    )
  }
}
