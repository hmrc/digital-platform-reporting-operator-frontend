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
import builders.UserAnswersBuilder.aUserAnswers
import models.operator.{AddressDetails, ContactDetails, NotificationType, TinDetails, TinType}
import models.operator.requests.{CreatePlatformOperatorRequest, Notification, UpdatePlatformOperatorRequest}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime

class CreateReportingNotificationAuditEventModelSpec extends SpecBase {

  "Create reporting notification - Success" in {

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

    val request = UpdatePlatformOperatorRequest(
      subscriptionId = "12345678900",
      operatorId = "some-operator-id",
      operatorName =  "C Company",
      tinDetails =  Seq(
        TinDetails("12345678900", TinType.Utr, "GB"),
        TinDetails("12345678900", TinType.Crn, "GB"),
        TinDetails("12345678900", TinType.Vrn, "GB"),
        TinDetails("12345678900", TinType.Empref, "GB"),
        TinDetails("12345678900", TinType.Chrn, "GB"),
      ),
      businessName = Some("C Company"),
      tradingName =  Some("The Simpsons Ltd."),
      primaryContactDetails =  ContactDetails(Some("075 23456789"), "Homer Simpson", "homer.simpson@example.com"),
      secondaryContactDetails =  Some(ContactDetails(Some("07952587369"), "Marge Simpson", "marge.simpson@example.com")),
      addressDetails =  AddressDetails(
        line1 = "742 Evergreen Terrace",
        line2 = Some("Second Terrace"),
        line3 = Some("Springfield"),
        line4 = None,
        postCode = None,
        countryCode = Some("AF")
      ),
      notification = Some(Notification(notificationType = NotificationType.Rpo,
        isActiveSeller = Some(true), isDueDiligence = None, firstPeriod = 2014))
    )

    val response = SuccessResponseData(
      processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")

    val auditEvent = CreateReportingNotificationAuditEventModel("AddPlatformOperator", request, response)

    Json.toJson(auditEvent) mustEqual expected

  }

  "Create platform operator - Failure" in {

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

    val request = UpdatePlatformOperatorRequest(
      subscriptionId = "12345678900",
      operatorId = "some-operator-id",
      operatorName =  "C Company",
      tinDetails =  Seq(
        TinDetails("12345678900", TinType.Utr, "GB"),
        TinDetails("12345678900", TinType.Crn, "GB"),
        TinDetails("12345678900", TinType.Vrn, "GB"),
        TinDetails("12345678900", TinType.Empref, "GB"),
        TinDetails("12345678900", TinType.Chrn, "GB"),
      ),
      businessName = Some("C Company"),
      tradingName =  Some("The Simpsons Ltd."),
      primaryContactDetails =  ContactDetails(Some("075 23456789"), "Homer Simpson", "homer.simpson@example.com"),
      secondaryContactDetails =  Some(ContactDetails(Some("07952587369"), "Marge Simpson", "marge.simpson@example.com")),
      addressDetails =  AddressDetails(
        line1 = "742 Evergreen Terrace",
        line2 = Some("Second Terrace"),
        line3 = Some("Springfield"),
        line4 = None,
        postCode = None,
        countryCode = Some("AF")
      ),
      notification = None
    )

    val response = FailureResponseData(
      statusCode = 422, processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), category = "Failure", reason = "Internal Server Error")

    val auditEvent = CreateReportingNotificationAuditEventModel("AddPlatformOperator", request, response)

    Json.toJson(auditEvent) mustEqual expected
  }

}
