/*
 * Copyright 2025 HM Revenue & Customs
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

package models.addressLookup

import models.{Country, UkAddress}
import play.api.i18n.Messages
import play.api.libs.json.{Format, JsPath, Json, Reads}
import services.CountryService

case class AddressLookupModel(organisation: Option[String], lines: Seq[String], postcode: Option[String], country: Country) {

  def toEdit: AmendAddressLookupModel = {
    val el = editLines
    AmendAddressLookupModel(
      organisation,
      el._1,
      el._2,
      el._3,
      el._4,
      postcode.getOrElse(""),
      country.code
    )
  }

  def toUkAddress: UkAddress = {
    val el = editLines2
    UkAddress(
      line1 = el._1.getOrElse(""),
      line2 = el._2,
      town = el._4.getOrElse(""),
      county = el._3,
      postCode = postcode.getOrElse(""),
      country = country
    )
  }

  private def editLines2: (Option[String], Option[String], Option[String], Option[String]) = {

    // if 1 line, then it could be line1, line2, line3 or town/city
    // if 2 lines again we can not identify town/city
    // if 3 lines again we can not identify town/city
    // if there are 4 lines then we know which is the town/city

    val res = lines.length match {
      case 4 => (lines.headOption, lines.lift(1), lines.lift(2), lines.lift(3))
      case 3 => (lines.headOption, lines.lift(1), None, lines.lift(2))
      case 2 => (lines.headOption, None, None, lines.lift(1))
      case 1 => (lines.headOption, None, None, None)
    }

    res
  }

  private def editLines: (String, Option[String], Option[String], String) = {
    val l1 = lines.headOption.getOrElse("")
    val l2 = if (lines.length > 2) lines.lift(1) else None
    val l3 = if (lines.length > 3) lines.lift(2) else None
    val l4 = lines.lastOption.getOrElse("")
    (l1, l2, l3, l4)
  }


}


object AddressLookupModel {
  val responseReads: Reads[AddressLookupModel] = Reads { json =>
    Reads.at[AddressLookupModel](JsPath \ "address").reads(json)
  }
  implicit val fmt: Format[AddressLookupModel] = Json.format[AddressLookupModel]

}

object AmendAddressLookupModel {

  implicit val fmt: Format[AmendAddressLookupModel] = Json.format[AmendAddressLookupModel]
}

case class AmendAddressLookupModel(
  organisation: Option[String],
  line1: String,
  line2: Option[String],
  line3: Option[String],
  town: String,
  postcode: String,
  country: String
) {

  def toConfirmableAddress(implicit messages: Messages): AddressLookupModel =
    AddressLookupModel(
      organisation,
      Seq(line1) ++ line2.toSeq ++ line3.toSeq ++ Seq(town),
      if (postcode.isEmpty) {
        None
      } else {
        Some(postcode)
      },
      Country(country, CountryService.find(country).map(_.name).getOrElse("United Kingdom"))
    )

}
