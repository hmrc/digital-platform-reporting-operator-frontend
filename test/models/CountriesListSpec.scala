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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class CountriesListSpec extends AnyFreeSpec with Matchers {

  val countryList: CountriesList = new CountriesList {}

  "CountryList" - {
    "must have only GB in country list for gbCountry" in {

      countryList.gbCountry mustEqual Country("GB", "United Kingdom")
      countryList.gbCountry must not be Country("US", "United States")
      countryList.gbCountry must not be Country("GG", "Guernsey")
      countryList.gbCountry must not be Country("IM", "The Isle of Man")
      countryList.gbCountry must not be Country("JE", "Jersey")
    }

    "must have only Jersey, Guernsey, Isle of Man for ukCountries" in {

      countryList.ukCountries mustEqual List(Country("GG", "Guernsey"),
        Country("IM", "The Isle of Man"),
        Country("JE", "Jersey"))
      countryList.ukCountries must not contain Country("GB", "United Kingdom")
      countryList.ukCountries must not contain Country("US", "United States")
    }

    "must have only International countries for internationalCountries" in {

      countryList.internationalCountries mustEqual
        countryList.allCountries.filterNot(x => x.code == "GG" || x.code == "IM" || x.code == "JE" || x.code == "GB")
      countryList.internationalCountries must not contain Country("GB", "United Kingdom")
      countryList.internationalCountries must not contain Country("GG", "Guernsey")
      countryList.internationalCountries must not contain Country("IM", "The Isle of Man")
      countryList.internationalCountries must not contain Country("JE", "Jersey")
    }

    "must have all countries except GB for nonUkInternationalCountries" in {

      countryList.nonUkInternationalCountries must not contain Country("GB", "United Kingdom")
    }
  }

}
