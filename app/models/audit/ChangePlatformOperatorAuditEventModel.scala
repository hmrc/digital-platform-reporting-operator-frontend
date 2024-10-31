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

import models.Country
import models.operator.TinType
import models.operator.requests.UpdatePlatformOperatorRequest
import models.operator.responses.PlatformOperator
import play.api.libs.json.{JsObject, Json, OWrites}

case class ChangePlatformOperatorAuditEventModel(original: PlatformOperator, updated: UpdatePlatformOperatorRequest){
  private def name = "ChangePlatformOperatorDetails"
  def toAuditModel: AuditModel[ChangePlatformOperatorAuditEventModel] = {
    AuditModel(name, this)
  }
}

object ChangePlatformOperatorAuditEventModel {

  implicit lazy val writes: OWrites[ChangePlatformOperatorAuditEventModel] = new OWrites[ChangePlatformOperatorAuditEventModel] {
    override def writes(o: ChangePlatformOperatorAuditEventModel): JsObject = {

      val originalJson = toJson1(o.original)
      val updatedJson = toJson2(o.updated)

      val changedFieldsInOriginal = JsObject(originalJson.fieldSet.diff(updatedJson.fieldSet).toSeq)
      val changedFieldsInUpdated = JsObject(updatedJson.fieldSet.diff(originalJson.fieldSet).toSeq)

      Json.obj(
        "from" -> changedFieldsInOriginal,
        "to" -> changedFieldsInUpdated
      )
    }
  }

  private def toJson1(info: PlatformOperator): JsObject = {

    val businessNameJson = Json.obj("businessName" -> info.operatorName)
    val tradingNameJson = info.tradingName
      .map(tradingName => Json.obj("hasBusinessTradingName" -> true, "businessTradingName" -> tradingName))
      .getOrElse(Json.obj("hasBusinessTradingName" -> false))

    val hasTaxIdentifier = if (info.tinDetails.nonEmpty) {Json.obj("hasTaxIdentificationNumber" -> true)} else {Json.obj("hasTaxIdentificationNumber" -> false)}
    val taxResidentInUk = if (info.tinDetails.exists(_.issuedBy == "GB")) {Json.obj("ukTaxResident" -> true)} else {Json.obj("ukTaxResident" -> false)}

    val utr = info.tinDetails.find(obj => obj.tinType == TinType.Utr).map(obj => Json.obj("ctUtr" -> obj.tin)).getOrElse(Json.obj())
    val crn = info.tinDetails.find(obj => obj.tinType == TinType.Crn).map(obj => Json.obj("companyRegistrationNumber" -> obj.tin)).getOrElse(Json.obj())
    val vrn = info.tinDetails.find(obj => obj.tinType == TinType.Vrn).map(obj => Json.obj("vrn" -> obj.tin)).getOrElse(Json.obj())
    val empRef = info.tinDetails.find(obj => obj.tinType == TinType.Empref).map(obj => Json.obj("employerPayeReferenceNumber" -> obj.tin)).getOrElse(Json.obj())
    val chrn = info.tinDetails.find(obj => obj.tinType == TinType.Chrn).map(obj => Json.obj("hmrcCharityReference" -> obj.tin)).getOrElse(Json.obj())
    val other = info.tinDetails.find(obj => obj.tinType == TinType.Other).map(obj => Json.obj("internationalTaxIdentifier" -> obj.tin)).getOrElse(Json.obj())

    val taxIdentifiers = if (info.tinDetails.exists(_.issuedBy == "GB"))
    {Json.obj("taxIdentifiers" -> {utr ++ crn ++ vrn ++ empRef ++ chrn ++ other})} else {Json.obj()}

    val internationalTaxIdentifier = if (info.tinDetails.exists(_.issuedBy != "GB"))
    {other} else {Json.obj()}

    val registeredBusinessAddressInUk = info.addressDetails.countryCode.map{ countryCode =>
      if (Country.ukCountries.map(_.code).contains(countryCode)) {Json.obj("registeredBusinessAddressInUk" -> true)} else {
        Json.obj("registeredBusinessAddressInUk" -> false)}
    }.getOrElse(Json.obj())

    val addressLine2 = info.addressDetails.line2.map {line2 => Json.obj("addressLine2" -> line2)}.getOrElse(Json.obj())
    val city = info.addressDetails.line3.map {line3 => Json.obj("city" -> line3)}.getOrElse(Json.obj())
    val region = info.addressDetails.line4.map {line4 => Json.obj("region" -> line4)}.getOrElse(Json.obj())
    val postCode = info.addressDetails.postCode.map {postCode => Json.obj("postCode" -> postCode)}.getOrElse(Json.obj())
    val countryCode = info.addressDetails.countryCode.map {countryCode => Json.obj("countryCode" -> countryCode)}.getOrElse(Json.obj())
    val country = info.addressDetails.countryCode.flatMap { countryCode => Country.allCountries.find(_.code == countryCode).map(c => c.name)}
    val countryName = info.addressDetails.countryCode.map {_ => Json.obj("country" -> country)}.getOrElse(Json.obj())

    val registeredBusinessAddress =
      Json.obj(
        "businessAddress" -> Json.obj(
          "addressLine1" -> info.addressDetails.line1
        ).++(addressLine2).++(city).++(region).++(postCode).++(countryCode).++(countryName)
      )

    val canPhonePrimaryContactJson = info.primaryContactDetails.phoneNumber.map(phoneNumber => Json.obj(
      "canPhonePrimaryContact" -> true,
      "primaryContactPhoneNumber" -> phoneNumber
    )).getOrElse(Json.obj("canPhonePrimaryContact" -> false))

    val primaryContactDetails = Json.obj(
      "primaryContactName" -> info.primaryContactDetails.contactName,
      "primaryContactEmail" -> info.primaryContactDetails.emailAddress,
    ) ++ canPhonePrimaryContactJson

    val hasSecondaryContactJson = if (info.secondaryContactDetails.nonEmpty) {Json.obj("hasSecondaryContact" -> true)} else {Json.obj("hasSecondaryContact" -> false)}

    val canPhoneSecondaryContactJson = info.secondaryContactDetails.map(secondaryContact =>
      secondaryContact.phoneNumber.map( phone =>
        Json.obj(
          "canPhoneSecondaryContact" -> true,
          "secondaryContactPhoneNumber" -> phone
        )).getOrElse(Json.obj("canPhoneSecondaryContact" -> false))
    ).getOrElse(Json.obj())

    val secondaryContactDetails = info.secondaryContactDetails.map(details => Json.obj(
      "secondaryContactName" -> details.contactName,
      "secondaryContactEmail" -> details.emailAddress,
    )).getOrElse(Json.obj()) ++ canPhoneSecondaryContactJson

    businessNameJson ++ tradingNameJson ++ hasTaxIdentifier ++ taxResidentInUk ++
      taxIdentifiers ++ internationalTaxIdentifier ++
      registeredBusinessAddressInUk ++ registeredBusinessAddress ++ primaryContactDetails ++
      hasSecondaryContactJson ++ secondaryContactDetails
  }

  private def toJson2(info: UpdatePlatformOperatorRequest): JsObject = {
    val businessNameJson = Json.obj("businessName" -> info.operatorName)
    val tradingNameJson = info.tradingName
      .map(tradingName => Json.obj("hasBusinessTradingName" -> true, "businessTradingName" -> tradingName))
      .getOrElse(Json.obj("hasBusinessTradingName" -> false))

    val hasTaxIdentifier = if (info.tinDetails.nonEmpty) {Json.obj("hasTaxIdentificationNumber" -> true)} else {Json.obj("hasTaxIdentificationNumber" -> false)}
    val taxResidentInUk = if (info.tinDetails.exists(_.issuedBy == "GB")) {Json.obj("ukTaxResident" -> true)} else {Json.obj("ukTaxResident" -> false)}

    val utr = info.tinDetails.find(obj => obj.tinType == TinType.Utr).map(obj => Json.obj("ctUtr" -> obj.tin)).getOrElse(Json.obj())
    val crn = info.tinDetails.find(obj => obj.tinType == TinType.Crn).map(obj => Json.obj("companyRegistrationNumber" -> obj.tin)).getOrElse(Json.obj())
    val vrn = info.tinDetails.find(obj => obj.tinType == TinType.Vrn).map(obj => Json.obj("vrn" -> obj.tin)).getOrElse(Json.obj())
    val empRef = info.tinDetails.find(obj => obj.tinType == TinType.Empref).map(obj => Json.obj("employerPayeReferenceNumber" -> obj.tin)).getOrElse(Json.obj())
    val chrn = info.tinDetails.find(obj => obj.tinType == TinType.Chrn).map(obj => Json.obj("hmrcCharityReference" -> obj.tin)).getOrElse(Json.obj())
    val other = info.tinDetails.find(obj => obj.tinType == TinType.Other).map(obj => Json.obj("internationalTaxIdentifier" -> obj.tin)).getOrElse(Json.obj())

    val taxIdentifiers = if (info.tinDetails.exists(_.issuedBy == "GB"))
    {Json.obj("taxIdentifiers" -> {utr ++ crn ++ vrn ++ empRef ++ chrn ++ other})} else {Json.obj()}

    val internationalTaxIdentifier = if (info.tinDetails.exists(_.issuedBy != "GB"))
    {other} else {Json.obj()}

    val registeredBusinessAddressInUk = info.addressDetails.countryCode.map{ countryCode =>
      if (Country.ukCountries.map(_.code).contains(countryCode)) {Json.obj("registeredBusinessAddressInUk" -> true)} else {
        Json.obj("registeredBusinessAddressInUk" -> false)}
    }.getOrElse(Json.obj())

    val addressLine2 = info.addressDetails.line2.map {line2 => Json.obj("addressLine2" -> line2)}.getOrElse(Json.obj())
    val city = info.addressDetails.line3.map {line3 => Json.obj("city" -> line3)}.getOrElse(Json.obj())
    val region = info.addressDetails.line4.map {line4 => Json.obj("region" -> line4)}.getOrElse(Json.obj())
    val postCode = info.addressDetails.postCode.map {postCode => Json.obj("postCode" -> postCode)}.getOrElse(Json.obj())
    val countryCode = info.addressDetails.countryCode.map {countryCode => Json.obj("countryCode" -> countryCode)}.getOrElse(Json.obj())
    val country = info.addressDetails.countryCode.flatMap { countryCode => Country.allCountries.find(_.code == countryCode).map(c => c.name)}
    val countryName = info.addressDetails.countryCode.map {_ => Json.obj("country" -> country)}.getOrElse(Json.obj())

    val registeredBusinessAddress =
      Json.obj(
        "businessAddress" -> Json.obj(
          "addressLine1" -> info.addressDetails.line1
        ).++(addressLine2).++(city).++(region).++(postCode).++(countryCode).++(countryName)
      )

    val canPhonePrimaryContactJson = info.primaryContactDetails.phoneNumber.map(phoneNumber => Json.obj(
      "canPhonePrimaryContact" -> true,
      "primaryContactPhoneNumber" -> phoneNumber
    )).getOrElse(Json.obj("canPhonePrimaryContact" -> false))

    val primaryContactDetails = Json.obj(
      "primaryContactName" -> info.primaryContactDetails.contactName,
      "primaryContactEmail" -> info.primaryContactDetails.emailAddress,
    ) ++ canPhonePrimaryContactJson

    val hasSecondaryContactJson = if (info.secondaryContactDetails.nonEmpty) {Json.obj("hasSecondaryContact" -> true)} else {Json.obj("hasSecondaryContact" -> false)}

    val canPhoneSecondaryContactJson = info.secondaryContactDetails.map(secondaryContact =>
      secondaryContact.phoneNumber.map( phone =>
        Json.obj(
          "canPhoneSecondaryContact" -> true,
          "secondaryContactPhoneNumber" -> phone
        )).getOrElse(Json.obj("canPhoneSecondaryContact" -> false))
    ).getOrElse(Json.obj())

    val secondaryContactDetails = info.secondaryContactDetails.map(details => Json.obj(
      "secondaryContactName" -> details.contactName,
      "secondaryContactEmail" -> details.emailAddress,
    )).getOrElse(Json.obj()) ++ canPhoneSecondaryContactJson

    businessNameJson ++ tradingNameJson ++ hasTaxIdentifier ++ taxResidentInUk ++
      taxIdentifiers ++ internationalTaxIdentifier ++
      registeredBusinessAddressInUk ++ registeredBusinessAddress ++ primaryContactDetails ++
      hasSecondaryContactJson ++ secondaryContactDetails
  }

}