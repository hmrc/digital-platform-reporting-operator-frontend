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

package models.operator

import models.{InternationalAddress, JerseyGuernseyIoMAddress, UkAddress}
import play.api.libs.json.{Json, OFormat}

final case class AddressDetails(line1: String,
                                line2: Option[String],
                                line3: Option[String],
                                line4: Option[String],
                                postCode: Option[String],
                                countryCode: Option[String])

object AddressDetails {
  implicit lazy val format: OFormat[AddressDetails] = Json.format

  def apply(ukAddress: UkAddress): AddressDetails = AddressDetails(
    line1 = ukAddress.line1,
    line2 = ukAddress.line2,
    line3 = Some(ukAddress.town),
    line4 = ukAddress.county,
    postCode = Some(ukAddress.postCode),
    countryCode = Some(ukAddress.country.code)
  )

  def apply(jerseyGuernseyIoMAddress: JerseyGuernseyIoMAddress): AddressDetails = AddressDetails(
    line1 = jerseyGuernseyIoMAddress.line1,
    line2 = jerseyGuernseyIoMAddress.line2,
    line3 = Some(jerseyGuernseyIoMAddress.town),
    line4 = jerseyGuernseyIoMAddress.county,
    postCode = Some(jerseyGuernseyIoMAddress.postCode),
    countryCode = Some(jerseyGuernseyIoMAddress.country.code)
  )

  def apply(internationalAddress: InternationalAddress): AddressDetails = AddressDetails(
    line1 = internationalAddress.line1,
    line2 = internationalAddress.line2,
    line3 = Some(internationalAddress.city),
    line4 = internationalAddress.region,
    postCode = Some(internationalAddress.postal),
    countryCode = Some(internationalAddress.country.code)
  )
}