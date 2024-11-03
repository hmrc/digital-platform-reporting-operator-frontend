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

package builders

import builders.SuccessResponseDataBuilder.aSuccessResponseData
import models.audit.CreatePlatformOperatorAuditEventModel
import models.operator.{AddressDetails, ContactDetails}
import models.operator.requests.CreatePlatformOperatorRequest

object AuditEventModelBuilder {

  val anAuditEventModel: CreatePlatformOperatorAuditEventModel = CreatePlatformOperatorAuditEventModel(
    requestData = CreatePlatformOperatorRequest(subscriptionId = "", operatorName = "", tinDetails = Seq.empty,
      businessName = None, tradingName = None, primaryContactDetails = ContactDetails(phoneNumber = None, contactName = "", emailAddress = ""),
      secondaryContactDetails = None, addressDetails = AddressDetails(line1 = "", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = None)
    ),
    responseData = aSuccessResponseData
  )
}
