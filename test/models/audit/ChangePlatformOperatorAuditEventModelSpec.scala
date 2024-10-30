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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json

import java.time.LocalDateTime

class ChangePlatformOperatorAuditEventModelSpec extends SpecBase {

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
