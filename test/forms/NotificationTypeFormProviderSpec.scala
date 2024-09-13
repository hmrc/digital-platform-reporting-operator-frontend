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

import forms.behaviours.OptionFieldBehaviours
import models.NotificationType
import play.api.data.FormError

class NotificationTypeFormProviderSpec extends OptionFieldBehaviours {

  val businessName = "name"
  val form = new NotificationTypeFormProvider()(businessName)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "notificationType.error.required"

    behave like optionsField[NotificationType](
      form,
      fieldName,
      validValues  = NotificationType.values,
      invalidError = FormError(fieldName, "error.invalid", Seq(businessName))
    )

    behave like mandatoryField(
      form,
      fieldName,
      FormError(fieldName, requiredKey, Seq(businessName))
    )
  }
}
