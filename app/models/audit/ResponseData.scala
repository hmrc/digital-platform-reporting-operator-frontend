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

import play.api.http.Status.OK
import play.api.libs.json.{Json, OWrites}

import java.time.LocalDateTime

sealed trait ResponseData

object ResponseData {

  implicit lazy val writes: OWrites[ResponseData] = {
    case success: SuccessResponseData => Json.toJsObject(success)
    case success: SuccessRemovalResponseData => Json.toJsObject(success)
    case failure: FailureResponseData => Json.toJsObject(failure)
  }
}

final case class SuccessResponseData(processedAt: LocalDateTime,
                                     platformOperatorId: String) extends ResponseData

object SuccessResponseData {
  implicit lazy val writes: OWrites[SuccessResponseData] =
    (o: SuccessResponseData) => Json.obj(
      "isSuccessful" -> true,
      "processedAt" -> o.processedAt,
      "platformOperatorId" -> o.platformOperatorId,
      "statusCode" -> OK
    )
}

final case class FailureResponseData(statusCode: Int,
                                     processedAt: LocalDateTime,
                                     category: String,
                                     reason: String) extends ResponseData

object FailureResponseData {
  implicit lazy val writes: OWrites[FailureResponseData] =
    (o: FailureResponseData) => Json.obj(
      "isSuccessful" -> false,
      "failureCategory" -> o.category,
      "failureReason" -> o.reason,
      "statusCode" -> o.statusCode,
      "processedAt" -> o.processedAt
    )
}

final case class SuccessRemovalResponseData(businessName: String,
                                     platformOperatorId: String) extends ResponseData

object SuccessRemovalResponseData {
  implicit lazy val writes: OWrites[SuccessRemovalResponseData] =
    (o: SuccessRemovalResponseData) => Json.obj(
      "confirmRemoval" -> true,
      "platformOperatorIdentifiers" -> Json.obj(
          "businessName" -> o.businessName,
          "platformOperatorId" -> o.platformOperatorId
      )
    )
}
