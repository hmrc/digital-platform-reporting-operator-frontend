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

import models.DefaultCountriesList
import models.operator.requests.CreatePlatformOperatorRequest
import models.operator.{AddressDetails, ContactDetails, TinDetails, TinType}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime

class CreatePlatformOperatorAuditEventModelSpec extends AnyFreeSpec with Matchers {

  private val countriesList = new DefaultCountriesList

  ".writes" - {
    val baseContact = ContactDetails(phoneNumber = None, contactName = "Homer Simpson", emailAddress = "homer.simpson@example.com")
    val baseAddress = AddressDetails(line1 = "742 Evergreen Terrace", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = Some("AF"))
    val basePlatformOperator = CreatePlatformOperatorRequest(
      subscriptionId = "12345678900",
      operatorName = "C Company",
      tinDetails = Seq(TinDetails("1234567890", TinType.Utr, "GB")),
      businessName = None,
      tradingName = None,
      primaryContactDetails = baseContact,
      secondaryContactDetails = None,
      addressDetails = baseAddress
    )

    "minimum data - with none uk registered address" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : false,
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "taxIdentifiers":{"ctUtr":"1234567890"},
          |    "registeredBusinessAddressInUk" : false,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "countryCode" : "AF",
          |      "country" : "Afghanistan"
          |    },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : false,
          |    "hasSecondaryContact" : false,
          |    "outcome": {
          |     "isSuccessful": true,
          |     "statusCode": 200,
          |     "platformOperatorId": "some-operator-id",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val responseData = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreatePlatformOperatorAuditEventModel(basePlatformOperator, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

    "minimum data - with uk registered address" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : false,
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "taxIdentifiers":{"ctUtr":"1234567890"},
          |    "registeredBusinessAddressInUk" : true,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "countryCode" : "JE",
          |      "country" : "Jersey"
          |    },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : false,
          |    "hasSecondaryContact" : false,
          |    "outcome": {
          |     "isSuccessful": true,
          |     "statusCode": 200,
          |     "platformOperatorId": "some-operator-id",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val requestData = basePlatformOperator.copy(addressDetails = AddressDetails(line1 = "742 Evergreen Terrace", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = Some("JE")))
      val responseData = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreatePlatformOperatorAuditEventModel(requestData, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

    "minimum data - with a business trading name" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : true,
          |    "taxIdentifiers":{"ctUtr":"1234567890"},
          |    "businessTradingName" : "Trading Name",
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "registeredBusinessAddressInUk" : false,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "countryCode" : "AF",
          |      "country" : "Afghanistan"
          |     },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : false,
          |    "hasSecondaryContact" : false,
          |    "outcome": {
          |     "isSuccessful": true,
          |     "statusCode": 200,
          |     "platformOperatorId": "some-operator-id",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val requestData = basePlatformOperator.copy(tradingName = Some("Trading Name"))
      val responseData = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreatePlatformOperatorAuditEventModel(requestData, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

    "minimum data - `hasTaxIdentifier` is true and `ukTaxResident` is true" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : false,
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "taxIdentifiers": {
          |       "ctUtr":"12345678900"
          |    },
          |    "registeredBusinessAddressInUk" : false,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "countryCode" : "AF",
          |      "country" : "Afghanistan"
          |     },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : false,
          |    "hasSecondaryContact" : false,
          |    "outcome": {
          |     "isSuccessful": true,
          |     "statusCode": 200,
          |     "platformOperatorId": "some-operator-id",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val requestData = basePlatformOperator.copy(tinDetails = Seq(TinDetails("12345678900", TinType.Utr, "GB")))
      val responseData = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreatePlatformOperatorAuditEventModel(requestData, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

    "minimum data - `registeredBusinessAddressInUk` is true" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : false,
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "taxIdentifiers":{"ctUtr":"1234567890"},
          |    "registeredBusinessAddressInUk" : true,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "addressLine2" : "Address line 2",
          |      "city" : "Address line 3",
          |      "region" : "Address line 4",
          |      "postCode" : "JE99 1JE",
          |      "countryCode" : "JE",
          |      "country" : "Jersey"
          |     },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : false,
          |    "hasSecondaryContact" : false,
          |    "outcome": {
          |     "isSuccessful": true,
          |     "statusCode": 200,
          |     "platformOperatorId": "some-operator-id",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val requestData = basePlatformOperator.copy(addressDetails =
        AddressDetails(
          line1 = "742 Evergreen Terrace",
          line2 = Some("Address line 2"),
          line3 = Some("Address line 3"),
          line4 = Some("Address line 4"),
          postCode = Some("JE99 1JE"),
          countryCode = Some("JE")
        )
      )
      val responseData = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreatePlatformOperatorAuditEventModel(requestData, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

    "minimum data - `canPhonePrimaryContact` is true" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : false,
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "taxIdentifiers":{"ctUtr":"1234567890"},
          |    "registeredBusinessAddressInUk" : false,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "countryCode" : "AF",
          |      "country" : "Afghanistan"
          |    },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : true,
          |    "primaryContactPhoneNumber" : "1234567890",
          |    "hasSecondaryContact" : false,
          |    "outcome": {
          |     "isSuccessful": true,
          |     "statusCode": 200,
          |     "platformOperatorId": "some-operator-id",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val requestData = basePlatformOperator.copy(primaryContactDetails = ContactDetails(phoneNumber = Some("1234567890"), contactName = "Homer Simpson", emailAddress = "homer.simpson@example.com"))
      val responseData = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreatePlatformOperatorAuditEventModel(requestData, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

    "minimum data - `hasSecondaryContact` is true" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : false,
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "taxIdentifiers":{"ctUtr":"1234567890"},
          |    "registeredBusinessAddressInUk" : false,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "countryCode" : "AF",
          |      "country" : "Afghanistan"
          |    },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : false,
          |    "hasSecondaryContact" : true,
          |    "secondaryContactName" : "Homer Simpson 2",
          |    "secondaryContactEmail" : "homer.simpson2@example.com",
          |    "canPhoneSecondaryContact" : false,
          |    "outcome": {
          |     "isSuccessful": true,
          |     "statusCode": 200,
          |     "platformOperatorId": "some-operator-id",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val requestData = basePlatformOperator.copy(
        secondaryContactDetails = Some(ContactDetails(phoneNumber = None, contactName = "Homer Simpson 2", emailAddress = "homer.simpson2@example.com")))
      val responseData = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreatePlatformOperatorAuditEventModel(requestData, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

    "minimum data - `hasSecondaryContact` is true `canPhoneSecondaryContact` is true" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : false,
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "taxIdentifiers":{"ctUtr":"1234567890"},
          |    "registeredBusinessAddressInUk" : false,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "countryCode" : "AF",
          |      "country" : "Afghanistan"
          |    },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : false,
          |    "hasSecondaryContact" : true,
          |    "secondaryContactName" : "Homer Simpson 2",
          |    "secondaryContactEmail" : "homer.simpson2@example.com",
          |    "canPhoneSecondaryContact" : true,
          |    "secondaryContactPhoneNumber" : "1234567890",
          |    "outcome": {
          |     "isSuccessful": true,
          |     "statusCode": 200,
          |     "platformOperatorId": "some-operator-id",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val requestData = basePlatformOperator.copy(
        secondaryContactDetails = Some(ContactDetails(phoneNumber = Some("1234567890"), contactName = "Homer Simpson 2", emailAddress = "homer.simpson2@example.com")))
      val responseData = SuccessResponseData(
        processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), platformOperatorId = "some-operator-id")
      val auditEvent = CreatePlatformOperatorAuditEventModel(requestData, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

    "Create platform operator - Failure" in {
      val expected = Json.parse(
        """
          |{
          |    "subscriptionId" : "12345678900",
          |    "businessName" : "C Company",
          |    "hasBusinessTradingName" : false,
          |    "hasTaxIdentificationNumber" : true,
          |    "ukTaxResident" : true,
          |    "taxIdentifiers":{"ctUtr":"1234567890"},
          |    "registeredBusinessAddressInUk" : false,
          |    "registeredBusinessAddress" : {
          |      "addressLine1" : "742 Evergreen Terrace",
          |      "countryCode" : "AF",
          |      "country" : "Afghanistan"
          |    },
          |    "primaryContactName" : "Homer Simpson",
          |    "primaryContactEmail" : "homer.simpson@example.com",
          |    "canPhonePrimaryContact" : false,
          |    "hasSecondaryContact" : false,
          |    "outcome": {
          |     "isSuccessful": false,
          |     "statusCode": 422,
          |     "failureCategory": "Failure",
          |     "failureReason": "Internal Server Error",
          |     "processedAt": "2001-01-01T02:30:23"
          |    }
          |  }
          |""".stripMargin).as[JsObject]

      val responseData = FailureResponseData(
        statusCode = 422, processedAt = LocalDateTime.of(2001, 1, 1, 2, 30, 23), category = "Failure", reason = "Internal Server Error")
      val auditEvent = CreatePlatformOperatorAuditEventModel(basePlatformOperator, responseData, countriesList)
      Json.toJson(auditEvent) mustEqual expected
    }

  }

}
