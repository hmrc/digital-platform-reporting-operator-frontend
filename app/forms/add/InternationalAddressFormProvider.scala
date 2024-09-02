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
import forms.mappings.Mappings
import models.{Country, InternationalAddress}
import play.api.data.Form
import play.api.data.Forms._

import javax.inject.Inject

class InternationalAddressFormProvider @Inject() extends Mappings {

  def apply(businessName: String): Form[InternationalAddress] = Form(
    mapping(
      "line1" -> text("internationalAddress.error.line1.required", args = Seq(businessName))
        .verifying(firstError(
          maxLength(35, "internationalAddress.error.line1.length"),
          regexp(Validation.textInputPattern.toString, "internationalAddress.error.line1.format")
        )),
      "line2" -> optional(text("")
        .verifying(firstError(
          maxLength(35, "internationalAddress.error.line2.length"),
          regexp(Validation.textInputPattern.toString, "internationalAddress.error.line2.format")
        ))),
      "city" -> text("internationalAddress.error.city.required")
        .verifying(firstError(
          maxLength(35, "internationalAddress.error.city.length"),
          regexp(Validation.textInputPattern.toString, "internationalAddress.error.city.format")
        )),
      "region" -> optional(text("")
        .verifying(firstError(
          maxLength(35, "internationalAddress.error.region.length"),
          regexp(Validation.textInputPattern.toString, "internationalAddress.error.region.format")
        ))),
      "postal" -> optional(text("")
        .verifying(firstError(
          maxLength(25, "internationalAddress.error.postal.length"),
          regexp(Validation.textInputPattern.toString, "internationalAddress.error.postal.format")
        ))),
      "country" -> text("internationalAddress.error.country.required")
        .verifying("internationalAddress.error.country.required", value => Country.internationalCountries.exists(_.code == value))
        .transform[Country](value => Country.internationalCountries.find(_.code == value).get, _.code)
    )(InternationalAddress.apply)(x => Some((x.line1, x.line2, x.city, x.region, x.postal, x.country)))
  )
}
