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
import config.FrontendAppConfig
import models.operator.requests.CreatePlatformOperatorRequest
import models.operator.responses.PlatformOperatorCreatedResponse
import models.operator.{AddressDetails, ContactDetails, TinDetails, TinType}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.await
import services.AuditService
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreatePlatformOperatorAuditEventModelSpec extends SpecBase {

  private val mockAuditConnector = mock[AuditConnector]
  private val mockAppConfig = mock[FrontendAppConfig]

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
        |      "registeredBusinessAddressInUk" : true,
        |      "registeredBusinessAddress" : {
        |      "addressLine1" : "742 Evergreen Terrace",
        |      "addressLine2" : " Second Terrace",
        |      "city" : "Springfield",
        |      "countryCode" : "AF",
        |      "countryName" : "Afghanistan"
        |    },
        |    "primaryContactName" : "Homer Simpson",
        |    "primaryContactEmailAddress" : "homer.simpson@example.com",
        |    "canPhonePrimaryContact" : true,
        |    "primaryContactPhoneNumber" : "075 23456789",
        |    "hasSecondaryContact" : true,
        |    "secondaryContactName" : "Marge Simpson",
        |    "secondaryContactEmailAddress" : "marge.simpson@example.com",
        |    "canPhoneSecondaryContact" : true,
        |    "secondaryContactPhoneNumber" : "07952587369",
        |     "outcome": {
        |     "isSuccessful": true,
        |     "statusCode": 200,
        |     "platformOperatorId": "some-operator-id",
        |     "processedAt": "2024-09-29T18:48:30.747881"
        |    }
        |  }
        |""".stripMargin).as[JsObject]

    val request = CreatePlatformOperatorRequest(
      subscriptionId = "12345678900",
      operatorName =  "C Company",
      tinDetails =  Seq(
        TinDetails("UTR1", TinType.Utr, "GB"),
        TinDetails("CRN1", TinType.Crn, "GB"),
        TinDetails("VRN1", TinType.Vrn, "GB"),
        TinDetails("ERN1", TinType.Empref, "GB"),
        TinDetails("CHARITY1", TinType.Chrn, "GB"),
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

    val response = PlatformOperatorCreatedResponse(operatorId = "some-operator-id")

    val auditEvent = CreatePlatformOperatorAuditEventModel(request, response)

//    Json.toJson(auditEvent) mustEqual expected

  }

}
