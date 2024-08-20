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

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import models.UkAddress

class UkAddressFormProvider @Inject() extends Mappings {

   def apply(businessName: String): Form[UkAddress] = Form(
     mapping(
      "line1" -> text("ukAddress.error.line1.required", args = Seq(businessName))
        .verifying(maxLength(100, "ukAddress.error.line1.length", args = businessName)),
      "line2" -> text("ukAddress.error.line2.required", args = Seq(businessName))
        .verifying(maxLength(100, "ukAddress.error.line2.length", args = businessName))
    )(UkAddress.apply)(x => Some((x.line1, x.line2)))
   )
 }
