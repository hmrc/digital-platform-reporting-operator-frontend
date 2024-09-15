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

import forms.mappings.Mappings
import play.api.data.Form

import java.time.{Clock, LocalDate, Month}
import javax.inject.Inject

class ReportingPeriodFormProvider @Inject()(clock: Clock) extends Mappings {

  def apply(businessName: String): Form[Int] = {

    // TODO: Confirm date limits
    val now = LocalDate.now(clock)
    val maxYear = now.getYear + 1
    val minYear = 2024

    Form(
      "value" -> int("reportingPeriod.error.required", args = Seq(businessName))
        .verifying(minimumValue(minYear, "reportingPeriod.error.belowMinimum", args = minYear.toString))
        .verifying(maximumValue(maxYear, "reportingPeriod.error.aboveMaximum", args = maxYear.toString))
    )
  }
}
