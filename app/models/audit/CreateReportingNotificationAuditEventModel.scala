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

import models.operator.NotificationType
import models.operator.requests.UpdatePlatformOperatorRequest
import play.api.libs.json.{JsObject, Json, OWrites}

import java.time.{Instant, LocalDateTime, ZoneId}

case class CreateReportingNotificationAuditEventModel(auditType: String, requestData: UpdatePlatformOperatorRequest, responseData: ResponseData) {
  private def name = "AddReportingNotification"
  def toAuditModel: AuditModel[CreateReportingNotificationAuditEventModel] = {
    AuditModel(name, this)
  }
}

object CreateReportingNotificationAuditEventModel {

  implicit lazy val writes: OWrites[CreateReportingNotificationAuditEventModel] = (o: CreateReportingNotificationAuditEventModel) =>
    toJson(o.requestData) + ("outcome" -> Json.toJson(o.responseData))

  private def toJson(info: UpdatePlatformOperatorRequest): JsObject = {
    val notificationJson = getNotification(info)
    notificationJson
  }

  private def getNotification(info: UpdatePlatformOperatorRequest): JsObject = {
    val isRPO = info.notification.exists(notification =>
      notification.notificationType == NotificationType.Rpo)
    val notificationType = info.notification.map(notification =>
      notification.notificationType match {
        case NotificationType.Rpo => "Reporting platform operator (RPO)"
        case NotificationType.Epo => "Excluded platform operator (EPO)"
      })
    val reportingNotificationType = info.notification.map(_ =>
      Json.obj("reportingNotificationType" -> notificationType)).getOrElse(Json.obj())
    val reportingPeriod = info.notification.map(period => Json.obj("reportingPeriod" -> period.firstPeriod)).getOrElse(Json.obj())
    val activeSellerDueDiligence = info.notification.flatMap(dueDiligence =>
      dueDiligence.isActiveSeller match {
        case Some(true) => Some("Active seller due diligence")
        case _ => None
      })
    val extendedDueDiligence = info.notification.flatMap(dueDiligence =>
      dueDiligence.isDueDiligence match {
        case Some(true) => Some("Extended due diligence")
        case _ => None
      })
    val listOfOptions = List(activeSellerDueDiligence, extendedDueDiligence).flatten
    val finalListOptions = if (listOfOptions.isEmpty) List("None of the above") else listOfOptions
    val typeOfDueDiligenceTaken = info.notification.map(_ =>
      Json.obj("typeOfDueDiligenceTaken" -> finalListOptions)
    ).getOrElse(Json.obj())
    reportingNotificationType ++ reportingPeriod ++ (if(isRPO) typeOfDueDiligenceTaken else Json.obj())
  }

  def apply(requestData: UpdatePlatformOperatorRequest, platformOperatorId: String): CreateReportingNotificationAuditEventModel = {
    val localDateTime = LocalDateTime.ofInstant(Instant.now, ZoneId.of("UTC"))
    val responseData = SuccessResponseData(localDateTime, platformOperatorId)
    CreateReportingNotificationAuditEventModel("AddReportingNotification", requestData, responseData)
  }

  def apply(requestData: UpdatePlatformOperatorRequest, status: Int): CreateReportingNotificationAuditEventModel = {
    val localDateTime = LocalDateTime.ofInstant(Instant.now, ZoneId.of("UTC"))
    val responseData = FailureResponseData(status, localDateTime, "Failure", "Internal Server Error")
    CreateReportingNotificationAuditEventModel("AddReportingNotification", requestData, responseData)
  }

}