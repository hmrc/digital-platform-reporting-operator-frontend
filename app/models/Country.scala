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

package models

import play.api.libs.json.{Json, OFormat}

final case class Country(code: String, name: String)

object Country {
  val Guernsey: Country = Country("GG", "Guernsey")
  val Jersey: Country = Country("JE", "Jersey")
  val IsleOfMan: Country = Country("IM", "Isle of Man")
  val UnitedKingdom: Country = Country("GB", "United Kingdom")

  implicit val format: OFormat[Country] = Json.format[Country]
}

case class FcoCountry(country: String, name: String)

object FcoCountry {
  implicit val formats: OFormat[FcoCountry] = Json.format[FcoCountry]
}