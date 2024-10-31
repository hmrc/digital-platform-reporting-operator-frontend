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

package models.audit

import base.SpecBase
import builders.AuditEventModelBuilder.anAuditEventModel
import builders.FailureResponseDataBuilder.aFailureResponseData
import builders.SuccessResponseDataBuilder.aSuccessResponseData
import models.operator.requests.UpdatePlatformOperatorRequest
import models.operator.{AddressDetails, ContactDetails, TinDetails, TinType}
import models.operator.responses.PlatformOperator
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json

import java.time.LocalDateTime

class ChangePlatformOperatorAuditEventModelSpec extends SpecBase {

  ".writes" - {

    val baseContact = ContactDetails(phoneNumber = None, contactName = "contactName", emailAddress = "emailAddress")
    val baseAddress = AddressDetails(line1 = "line1", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = None)
    val baseOriginalInfo = PlatformOperator(
      operatorId = "operatorId",
      operatorName = "operatorName",
      tinDetails = Seq.empty,
      businessName = None,
      tradingName = None,
      primaryContactDetails = baseContact,
      secondaryContactDetails = None,
      addressDetails = baseAddress,
      notifications = Seq.empty
    )
    val baseUpdateInfo = UpdatePlatformOperatorRequest(
      subscriptionId = "subscriptionId",
      operatorId = "operatorId",
      operatorName = "operatorName",
      tinDetails = Seq.empty,
      businessName = None,
      tradingName = None,
      primaryContactDetails = baseContact,
      secondaryContactDetails = None,
      addressDetails = baseAddress,
      notification = None
    )

    "when business name has changed" - {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(businessName = Some("businessName"))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(),
        "to" -> Json.obj(
          "businessName" -> "businessName"
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when `hasBusinessTradingName` from false to true" in {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(tradingName = Some("tradingName"))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasBusinessTradingName" -> false
        ),
        "to" -> Json.obj(
          "hasBusinessTradingName" -> true,
          "businessTradingName" -> "tradingName"
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when `hasBusinessTradingName` from true to false" in {
      val original = baseOriginalInfo.copy(tradingName = Some("tradingName"))
      val updated = baseUpdateInfo
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasBusinessTradingName" -> true,
          "businessTradingName" -> "tradingName"
        ),
        "to" -> Json.obj(
          "hasBusinessTradingName" -> false
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when `hasTaxIdentifier` from false to true" in {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(tinDetails = Seq(TinDetails("1234567890",TinType.Utr,"GB")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasTaxIdentificationNumber" -> false,
          "ukTaxResident" -> false
        ),
        "to" -> Json.obj(
          "hasTaxIdentificationNumber" -> true,
          "ukTaxResident" -> true,
          "taxIdentifiers" -> Json.obj(
            "ctUtr" -> "1234567890"
          ),
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when `hasTaxIdentifier` from true to false" in {
      val original = baseOriginalInfo.copy(tinDetails = Seq(TinDetails("1234567890",TinType.Utr,"GB")))
      val updated = baseUpdateInfo
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasTaxIdentificationNumber" -> true,
          "ukTaxResident" -> true,
          "taxIdentifiers" -> Json.obj(
            "ctUtr" -> "1234567890"
        )),
        "to" -> Json.obj(
          "hasTaxIdentificationNumber" -> false,
          "ukTaxResident" -> false
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }


  }


  "Audit event" - {
    "must serialise correctly with success data" in {
      val auditEventModel = anAuditEventModel.copy(
        auditType = "some-audit-type",
        requestData = Json.obj(),
        responseData = aSuccessResponseData.copy(
          processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23),
          platformOperatorId = "some-platform-operator-id"
        )
      )

      Json.toJson(auditEventModel) mustBe Json.obj(
        "outcome" -> Json.obj(
          "isSuccessful" -> true,
          "processedAt" -> "2001-01-01T02:30:23",
          "platformOperatorId" -> "some-platform-operator-id",
          "statusCode" -> OK
        )
      )
    }

    "must serialise correctly with failure data" in {
      val auditEventModel = anAuditEventModel.copy(
        auditType = "some-audit-type",
        requestData = Json.obj(),
        responseData = aFailureResponseData.copy(
          processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23),
        )
      )

      Json.toJson(auditEventModel) mustBe Json.obj(
        "outcome" -> Json.obj(
          "isSuccessful" -> false,
          "failureCategory" -> "default-category",
          "failureReason" -> "default-reason",
          "statusCode" -> INTERNAL_SERVER_ERROR,
          "processedAt" -> "2001-01-01T02:30:23"
        )
      )
    }
  }

}
