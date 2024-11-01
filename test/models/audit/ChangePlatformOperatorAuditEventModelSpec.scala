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
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime

class ChangePlatformOperatorAuditEventModelSpec extends SpecBase {

  ".writes" - {

    val baseContact = ContactDetails(phoneNumber = None, contactName = "contactName", emailAddress = "emailAddress")
    val baseAddress = AddressDetails(line1 = "line1", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = None)
    val baseOriginalInfo = PlatformOperator(
      operatorId = "operatorId",
      operatorName = "businessName",
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
      operatorName = "businessName",
      tinDetails = Seq.empty,
      businessName = None,
      tradingName = None,
      primaryContactDetails = baseContact,
      secondaryContactDetails = None,
      addressDetails = baseAddress,
      notification = None
    )

    "when business name has changed" in {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(operatorName = "newBusinessName")
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "businessName" -> "businessName"
        ),
        "to" -> Json.obj(
          "businessName" -> "newBusinessName"
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

    "when `hasTaxIdentifier` from false to true for a UK tax resident" in {
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

    "when `hasTaxIdentifier` from true to false for a existing UK tax resident" in {
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

    "when `hasTaxIdentifier` from false to true for a non UK tax resident" in {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(tinDetails = Seq(TinDetails("9999",TinType.Other,"US")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasTaxIdentificationNumber" -> false
        ),
        "to" -> Json.obj(
          "hasTaxIdentificationNumber" -> true,
          "internationalTaxIdentifier" -> "9999"
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when `hasTaxIdentifier` from true to false for a existing non UK tax resident" in {
      val original = baseOriginalInfo.copy(tinDetails = Seq(TinDetails("9999",TinType.Other,"US")))
      val updated = baseUpdateInfo
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasTaxIdentificationNumber" -> true,
          "internationalTaxIdentifier" -> "9999"
        ),
        "to" -> Json.obj(
          "hasTaxIdentificationNumber" -> false,
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when changes applied to a UK tax identification identifier value" in {
      val original = baseOriginalInfo.copy(tinDetails = Seq(TinDetails("1234567890",TinType.Utr,"GB")))
      val updated = baseUpdateInfo.copy(tinDetails = Seq(TinDetails("0987654321",TinType.Utr,"GB")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "taxIdentifiers" -> Json.obj(
            "ctUtr" -> "1234567890"
          )),
        "to" -> Json.obj(
          "taxIdentifiers" -> Json.obj(
            "ctUtr" -> "0987654321"
          )),
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when an additional UK tax identification identifier is added" in {
      val original = baseOriginalInfo.copy(tinDetails = Seq(TinDetails("1234567890",TinType.Utr,"GB")))
      val updated = baseUpdateInfo.copy(tinDetails = Seq(TinDetails("1234567890",TinType.Utr,"GB"), TinDetails("AB123456",TinType.Crn,"GB")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "taxIdentifiers" -> Json.obj(
            "ctUtr" -> "1234567890"
          )),
        "to" -> Json.obj(
          "taxIdentifiers" -> Json.obj(
            "ctUtr" -> "1234567890",
            "companyRegistrationNumber" -> "AB123456"
          )),
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when 'registeredBusinessAddressInUk' from false to true " in {
      val original = baseOriginalInfo.copy(addressDetails =
        AddressDetails(line1 = "line1", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = Some("US")))
      val updated = baseUpdateInfo.copy(addressDetails =
        AddressDetails(line1 = "line1", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = Some("GB")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "registeredBusinessAddressInUk" -> false,
          "businessAddress" -> Json.obj(
            "addressLine1" -> "line1",
            "countryCode" -> "US",
            "country" -> "United States"
          )
        ),
        "to" -> Json.obj(
          "registeredBusinessAddressInUk" -> true,
          "businessAddress" -> Json.obj(
            "addressLine1" -> "line1",
            "countryCode" -> "GB",
            "country" -> "United Kingdom"
          )
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when 'registeredBusinessAddressInUk' from true to false " in {
      val original = baseOriginalInfo.copy(addressDetails =
        AddressDetails(line1 = "line1", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = Some("GB")))
      val updated = baseUpdateInfo.copy(addressDetails =
        AddressDetails(line1 = "line1", line2 = None, line3 = None, line4 = None, postCode = None, countryCode = Some("US")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "registeredBusinessAddressInUk" -> true,
          "businessAddress" -> Json.obj(
            "addressLine1" -> "line1",
            "countryCode" -> "GB",
            "country" -> "United Kingdom"
          )
        ),
        "to" -> Json.obj(
          "registeredBusinessAddressInUk" -> false,
          "businessAddress" -> Json.obj(
            "addressLine1" -> "line1",
            "countryCode" -> "US",
            "country" -> "United States"
          )
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }


    "when 'businessAddress' has a change " in {
      val original = baseOriginalInfo.copy(addressDetails =
        AddressDetails(line1 = "line1", line2 = Some("22 Street"), line3 = None, line4 = None, postCode = None, countryCode = Some("GB")))
      val updated = baseUpdateInfo.copy(addressDetails =
        AddressDetails(line1 = "line1", line2 = Some("33 Street"), line3 = None, line4 = None, postCode = None, countryCode = Some("GB")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "businessAddress" -> Json.obj(
            "addressLine1" -> "line1",
            "addressLine2" -> "22 Street",
            "countryCode" -> "GB",
            "country" -> "United Kingdom"
          )
        ),
        "to" -> Json.obj(
          "businessAddress" -> Json.obj(
            "addressLine1" -> "line1",
            "addressLine2" -> "33 Street",
            "countryCode" -> "GB",
            "country" -> "United Kingdom"
          )
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when primary contact name has changed" in {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(primaryContactDetails = ContactDetails(None, "NewName", "emailAddress"))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "primaryContactName" -> "contactName"
        ),
        "to" -> Json.obj(
          "primaryContactName" -> "NewName",
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when primary contact email has changed" in {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(primaryContactDetails = ContactDetails(None, "contactName", "newEmailAddress"))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "primaryContactEmail" -> "emailAddress"
        ),
        "to" -> Json.obj(
          "primaryContactEmail" -> "newEmailAddress",
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when 'canPhonePrimaryContact' false to true" in {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(primaryContactDetails = ContactDetails(Some("1234567890"), "contactName", "emailAddress"))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "canPhonePrimaryContact" -> false
        ),
        "to" -> Json.obj(
          "canPhonePrimaryContact" -> true,
          "primaryContactPhoneNumber" -> "1234567890"
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when 'canPhonePrimaryContact' true to false" in {
      val original = baseOriginalInfo.copy(primaryContactDetails = ContactDetails(Some("1234567890"), "contactName", "emailAddress"))
      val updated = baseUpdateInfo.copy(primaryContactDetails = ContactDetails(None, "contactName", "emailAddress"))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "canPhonePrimaryContact" -> true,
          "primaryContactPhoneNumber" -> "1234567890"
        ),
        "to" -> Json.obj(
          "canPhonePrimaryContact" -> false,
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when 'hasSecondaryContact' false to true" in {
      val original = baseOriginalInfo
      val updated = baseUpdateInfo.copy(secondaryContactDetails = Some(ContactDetails(None, "contactName", "emailAddress")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasSecondaryContact" -> false
        ),
        "to" -> Json.obj(
          "hasSecondaryContact" -> true,
          "canPhoneSecondaryContact" -> false,
          "secondaryContactName" -> "contactName",
          "secondaryContactEmail" -> "emailAddress"
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when 'hasSecondaryContact' true to false" in {
      val original = baseOriginalInfo.copy(secondaryContactDetails = Some(ContactDetails(None, "contactName", "emailAddress")))
      val updated = baseUpdateInfo
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasSecondaryContact" -> true,
          "canPhoneSecondaryContact" -> false,
          "secondaryContactName" -> "contactName",
          "secondaryContactEmail" -> "emailAddress"
        ),
        "to" -> Json.obj(
          "hasSecondaryContact" -> false
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when 'canPhoneSecondaryContact' false to true" in {
      val original = baseOriginalInfo.copy(secondaryContactDetails = Some(ContactDetails(None, "contactName", "emailAddress")))
      val updated = baseUpdateInfo.copy(secondaryContactDetails = Some(ContactDetails(Some("1234567890"), "contactName", "emailAddress")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "canPhoneSecondaryContact" -> false,
        ),
        "to" -> Json.obj(
          "canPhoneSecondaryContact" -> true,
          "secondaryContactPhoneNumber" -> "1234567890"
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

    "when 'canPhoneSecondaryContact' true to false" in {
      val original = baseOriginalInfo.copy(secondaryContactDetails = Some(ContactDetails(Some("1234567890"), "contactName", "emailAddress")))
      val updated = baseUpdateInfo.copy(secondaryContactDetails = Some(ContactDetails(None, "contactName", "emailAddress")))
      val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "canPhoneSecondaryContact" -> true,
          "secondaryContactPhoneNumber" -> "1234567890"
        ),
        "to" -> Json.obj(
          "canPhoneSecondaryContact" -> false
        )
      )
      Json.toJson(auditEvent) mustEqual expectedJson
    }

  }

  "Change platform operator - Success" in {
    val expected = Json.parse(
      """
        |{
        | "from":{
        |   "businessName":"C Company"
        |  },
        | "to":{
        |   "businessName":"C Company New"
        |  }
        |}
      """.stripMargin).as[JsObject]

    val original = PlatformOperator(
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
      notifications = Seq.empty
    )

    val updated = UpdatePlatformOperatorRequest(
      subscriptionId = "12345678900",
      operatorId = "some-operator-id",
      operatorName =  "C Company New",
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

    val auditEvent = ChangePlatformOperatorAuditEventModel(original, updated)

    Json.toJson(auditEvent) mustEqual expected

  }

}