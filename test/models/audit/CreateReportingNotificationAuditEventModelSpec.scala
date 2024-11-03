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
import models.operator.{AddressDetails, ContactDetails, NotificationType}
import models.operator.requests.{Notification, UpdatePlatformOperatorRequest}
import play.api.libs.json.Json

import java.time.LocalDateTime

class CreateReportingNotificationAuditEventModelSpec extends SpecBase {

  ".writes" - {

    val baseContact = ContactDetails(phoneNumber = None, contactName = "contactName", emailAddress = "emailAddress")
    val baseAddress = AddressDetails(line1 = "line1", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = None)
    val baseUpdatePlatformOperator = UpdatePlatformOperatorRequest(
      subscriptionId = "subscription-id",
      operatorId = "operator-id",
      operatorName = "operator-name",
      tinDetails = Seq.empty,
      businessName = None,
      tradingName = None,
      primaryContactDetails = baseContact,
      secondaryContactDetails = None,
      addressDetails = baseAddress,
      notification = None
    )

    "Reporting platform operator (RPO)" - {

      "No due diligence" in {
        val expected = Json.parse(
          """
            |{
            | "reportingNotificationType":"Reporting platform operator (RPO)",
            | "reportingPeriod":2014,
            | "typeOfDueDiligenceTaken":["None of the above"],
            | "outcome":{
            |   "isSuccessful":true,
            |   "processedAt":"2001-01-01T02:30:23",
            |   "platformOperatorId":"some-operator-id",
            |   "statusCode":200}
            | }
            |""".stripMargin
        )
        val request = baseUpdatePlatformOperator.copy(notification = Some(Notification(notificationType = NotificationType.Rpo,
          isActiveSeller = None, isDueDiligence = None, firstPeriod = 2014)))
        val response = SuccessResponseData(
          processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
        val auditEvent = CreateReportingNotificationAuditEventModel(request, response)
        Json.toJson(auditEvent) mustEqual expected
      }

      "Active seller due diligence" in {
        val expected = Json.parse(
          """
            |{
            | "reportingNotificationType":"Reporting platform operator (RPO)",
            | "reportingPeriod":2014,
            | "typeOfDueDiligenceTaken":["Active seller due diligence"],
            | "outcome":{
            |   "isSuccessful":true,
            |   "processedAt":"2001-01-01T02:30:23",
            |   "platformOperatorId":"some-operator-id",
            |   "statusCode":200}
            | }
            |""".stripMargin
        )
        val request = baseUpdatePlatformOperator.copy(notification = Some(Notification(notificationType = NotificationType.Rpo,
          isActiveSeller = Some(true), isDueDiligence = None, firstPeriod = 2014)))
        val response = SuccessResponseData(
          processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
        val auditEvent = CreateReportingNotificationAuditEventModel(request, response)
        Json.toJson(auditEvent) mustEqual expected
      }

      "Extended due diligence" in {
        val expected = Json.parse(
          """
            |{
            | "reportingNotificationType":"Reporting platform operator (RPO)",
            | "reportingPeriod":2014,
            | "typeOfDueDiligenceTaken":["Extended due diligence"],
            | "outcome":{
            |   "isSuccessful":true,
            |   "processedAt":"2001-01-01T02:30:23",
            |   "platformOperatorId":"some-operator-id",
            |   "statusCode":200}
            | }
            |""".stripMargin
        )
        val request = baseUpdatePlatformOperator.copy(notification = Some(Notification(notificationType = NotificationType.Rpo,
          isActiveSeller = None, isDueDiligence = Some(true), firstPeriod = 2014)))
        val response = SuccessResponseData(
          processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
        val auditEvent = CreateReportingNotificationAuditEventModel(request, response)
        Json.toJson(auditEvent) mustEqual expected
      }

      "Active seller due diligence, Extended due diligence" in {
        val expected = Json.parse(
          """
            |{
            | "reportingNotificationType":"Reporting platform operator (RPO)",
            | "reportingPeriod":2014,
            | "typeOfDueDiligenceTaken":["Active seller due diligence", "Extended due diligence"],
            | "outcome":{
            |   "isSuccessful":true,
            |   "processedAt":"2001-01-01T02:30:23",
            |   "platformOperatorId":"some-operator-id",
            |   "statusCode":200}
            | }
            |""".stripMargin
        )
        val request = baseUpdatePlatformOperator.copy(notification = Some(Notification(notificationType = NotificationType.Rpo,
          isActiveSeller = Some(true), isDueDiligence = Some(true), firstPeriod = 2014)))
        val response = SuccessResponseData(
          processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
        val auditEvent = CreateReportingNotificationAuditEventModel(request, response)
        Json.toJson(auditEvent) mustEqual expected
      }

    }

    "Excluded platform operator (EPO)" in {

      val expected = Json.parse(
        """
          |{
          | "reportingNotificationType":"Excluded platform operator (EPO)",
          | "reportingPeriod":2014,
          | "outcome":{
          |   "isSuccessful":true,
          |   "processedAt":"2001-01-01T02:30:23",
          |   "platformOperatorId":"some-operator-id",
          |   "statusCode":200}
          | }
          |""".stripMargin
      )
      val request = baseUpdatePlatformOperator.copy(notification = Some(Notification(notificationType = NotificationType.Epo,
        isActiveSeller = Some(true), isDueDiligence = Some(true), firstPeriod = 2014)))
      val response = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreateReportingNotificationAuditEventModel(request, response)
      Json.toJson(auditEvent) mustEqual expected

    }

    "Create reporting notification - Failure" in {

      val expected = Json.parse(
        """
          |{
          | "outcome":{
          |  "isSuccessful":false,
          |  "failureCategory":"Failure",
          |  "failureReason":"Internal Server Error",
          |  "statusCode":422,
          |  "processedAt":"2001-01-01T02:30:23"
          |  }
          |}
          |""".stripMargin
      )

      val request = baseUpdatePlatformOperator
      val response = FailureResponseData(
        statusCode = 422, processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), category = "Failure", reason = "Internal Server Error")
      val auditEvent = CreateReportingNotificationAuditEventModel(request, response)

      Json.toJson(auditEvent) mustEqual expected

    }

  }

}
