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
import models.operator.requests.CreatePlatformOperatorRequest
import models.operator.{AddressDetails, ContactDetails, TinDetails, TinType}
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime

class CreatePlatformOperatorAuditEventModelSpec extends SpecBase {

  "Create platform operator - Success" in {
    val expected = Json.parse(
      """
        |{
        |    "subscriptionId" : "12345678900",
        |    "businessName" : "C Company",
        |    "hasBusinessTradingName" : true,
        |    "businessTradingName" : "The Simpsons Ltd.",
        |    "hasTaxIdentificationNumber" : true,
        |    "ukTaxResident" : true,
        |     "taxIdentifiers" : {
        |      "ctUtr" : "12345678900",
        |      "companyRegistrationNumber" : "12345678900",
        |      "vrn" : "12345678900",
        |      "employerPayeReferenceNumber" : "12345678900",
        |      "hmrcCharityReference" : "12345678900"
        |    },
        |      "registeredBusinessAddressInUk" : false,
        |      "businessAddress" : {
        |      "addressLine1" : "742 Evergreen Terrace",
        |      "addressLine2" : "Second Terrace",
        |      "city" : "Springfield",
        |      "countryCode" : "AF",
        |      "country" : "Afghanistan"
        |    },
        |    "primaryContactName" : "Homer Simpson",
        |    "primaryContactEmail" : "homer.simpson@example.com",
        |    "canPhonePrimaryContact" : true,
        |    "primaryContactPhoneNumber" : "075 23456789",
        |    "hasSecondaryContact" : true,
        |    "secondaryContactName" : "Marge Simpson",
        |    "secondaryContactEmail" : "marge.simpson@example.com",
        |    "canPhoneSecondaryContact" : true,
        |    "secondaryContactPhoneNumber" : "07952587369",
        |     "outcome": {
        |     "isSuccessful": true,
        |     "statusCode": 200,
        |     "platformOperatorId": "some-operator-id",
        |     "processedAt": "2001-01-01T02:30:23"
        |    }
        |  }
        |""".stripMargin).as[JsObject]

    val request = CreatePlatformOperatorRequest(
      subscriptionId = "12345678900",
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
      )
    )

    val response = SuccessResponseData(
      processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")

    val auditEvent = CreatePlatformOperatorAuditEventModel("AddPlatformOperator", request, response)

    Json.toJson(auditEvent) mustEqual expected

  }

  "Create platform operator - Failure" in {
    val expected = Json.parse(
      """
        |{
        |    "subscriptionId" : "12345678900",
        |    "businessName" : "C Company",
        |    "hasBusinessTradingName" : true,
        |    "businessTradingName" : "The Simpsons Ltd.",
        |    "hasTaxIdentificationNumber" : true,
        |    "ukTaxResident" : true,
        |     "taxIdentifiers" : {
        |      "ctUtr" : "12345678900",
        |      "companyRegistrationNumber" : "12345678900",
        |      "vrn" : "12345678900",
        |      "employerPayeReferenceNumber" : "12345678900",
        |      "hmrcCharityReference" : "12345678900"
        |    },
        |      "registeredBusinessAddressInUk" : false,
        |      "businessAddress" : {
        |      "addressLine1" : "742 Evergreen Terrace",
        |      "addressLine2" : "Second Terrace",
        |      "city" : "Springfield",
        |      "countryCode" : "AF",
        |      "country" : "Afghanistan"
        |    },
        |    "primaryContactName" : "Homer Simpson",
        |    "primaryContactEmail" : "homer.simpson@example.com",
        |    "canPhonePrimaryContact" : true,
        |    "primaryContactPhoneNumber" : "075 23456789",
        |    "hasSecondaryContact" : true,
        |    "secondaryContactName" : "Marge Simpson",
        |    "secondaryContactEmail" : "marge.simpson@example.com",
        |    "canPhoneSecondaryContact" : true,
        |    "secondaryContactPhoneNumber" : "07952587369",
        |     "outcome": {
        |     "isSuccessful": false,
        |     "statusCode": 422,
        |     "failureCategory": "Failure",
        |     "failureReason": "Internal Server Error",
        |     "processedAt": "2001-01-01T02:30:23"
        |    }
        |  }
        |""".stripMargin).as[JsObject]

      val request = CreatePlatformOperatorRequest(
        subscriptionId = "12345678900",
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
        )
      )

      val response = FailureResponseData(
        statusCode = 422, processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), category = "Failure", reason = "Internal Server Error")

      val auditEvent = CreatePlatformOperatorAuditEventModel("AddPlatformOperator", request, response)

      Json.toJson(auditEvent) mustEqual expected
  }


}
