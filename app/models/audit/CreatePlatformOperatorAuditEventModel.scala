/*
 * Copyright 2023 HM Revenue & Customs
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

import models.operator.responses.PlatformOperatorCreatedResponse
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsObject, Json, OWrites}

import java.time.{Instant, LocalDateTime, ZoneId}

case class CreatePlatformOperatorAuditEventModel(auditType: String, requestData: JsObject, responseData: ResponseData) {
  private def name = "AddPlatformOperator"
  def toAuditModel: AuditModel[CreatePlatformOperatorAuditEventModel] = {
    AuditModel(name, this)
  }
}

object CreatePlatformOperatorAuditEventModel {

  implicit lazy val writes: OWrites[CreatePlatformOperatorAuditEventModel] = (o: CreatePlatformOperatorAuditEventModel) =>
    o.requestData + ("outcome" -> Json.toJson(o.responseData))

  def apply(requestData: JsObject, platformOperatorCreatedResponse: PlatformOperatorCreatedResponse): CreatePlatformOperatorAuditEventModel = {
    val localDateTime = LocalDateTime.ofInstant(Instant.now, ZoneId.of("UTC"))
    val responseData = SuccessResponseData(localDateTime, platformOperatorCreatedResponse.operatorId)
    CreatePlatformOperatorAuditEventModel("AddPlatformOperator", requestData, responseData)
  }

  def apply(requestData: JsObject): CreatePlatformOperatorAuditEventModel = {
    val localDateTime = LocalDateTime.ofInstant(Instant.now, ZoneId.of("UTC"))
    val responseData = FailureResponseData(INTERNAL_SERVER_ERROR, localDateTime, "Failure", "Internal Server Error")
    CreatePlatformOperatorAuditEventModel("AddPlatformOperator", requestData, responseData)
  }

}