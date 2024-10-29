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

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsObject, Json, OWrites}

import java.time.{Instant, LocalDateTime, ZoneId}

case class CreateReportingNotificationAuditEventModel(auditType: String, requestData: JsObject, responseData: ResponseData) {
  private def name = "AddReportingNotification"
  def toAuditModel: AuditModel[CreateReportingNotificationAuditEventModel] = {
    AuditModel(name, this)
  }
}

object CreateReportingNotificationAuditEventModel {

  implicit lazy val writes: OWrites[CreateReportingNotificationAuditEventModel] = (o: CreateReportingNotificationAuditEventModel) =>
    o.requestData + ("outcome" -> Json.toJson(o.responseData))

  def apply(requestData: JsObject, platformOperatorId: String): CreateReportingNotificationAuditEventModel = {
    val localDateTime = LocalDateTime.ofInstant(Instant.now, ZoneId.of("UTC"))
    val responseData = SuccessResponseData(localDateTime, platformOperatorId)
    CreateReportingNotificationAuditEventModel("AddReportingNotification", requestData, responseData)
  }

  def apply(requestData: JsObject): CreateReportingNotificationAuditEventModel = {
    val localDateTime = LocalDateTime.ofInstant(Instant.now, ZoneId.of("UTC"))
    val responseData = FailureResponseData(INTERNAL_SERVER_ERROR, localDateTime, "Failure", "Internal Server Error")
    CreateReportingNotificationAuditEventModel("AddPlatformOperator", requestData, responseData)
  }

}