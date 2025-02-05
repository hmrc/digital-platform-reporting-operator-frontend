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

package models.operator

import builders.InternationalAddressBuilder.anInternationalAddress
import builders.JerseyGuernseyIoMAddressBuilder.aJerseyGuernseyIoMAddress
import builders.UkAddressBuilder.aUkAddress
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AddressDetailsSpec extends AnyFreeSpec with Matchers {

  ".apply(ukAddress: UkAddress)" - {
    "must return correct object" in {
      AddressDetails.apply(aUkAddress) mustBe AddressDetails(
        line1 = aUkAddress.line1,
        line2 = aUkAddress.line2,
        line3 = Some(aUkAddress.town),
        line4 = aUkAddress.county,
        postCode = Some(aUkAddress.postCode),
        countryCode = Some(aUkAddress.country.code)
      )
    }
  }

  ".apply(jerseyGuernseyIoMAddress: JerseyGuernseyIoMAddress)" - {
    "must return correct object" in {
      AddressDetails.apply(aJerseyGuernseyIoMAddress) mustBe AddressDetails(
        line1 = aJerseyGuernseyIoMAddress.line1,
        line2 = aJerseyGuernseyIoMAddress.line2,
        line3 = Some(aJerseyGuernseyIoMAddress.town),
        line4 = aJerseyGuernseyIoMAddress.county,
        postCode = Some(aJerseyGuernseyIoMAddress.postCode),
        countryCode = Some(aJerseyGuernseyIoMAddress.country.code)
      )
    }
  }

  ".apply(internationalAddress: InternationalAddress)" - {
    "must return correct object" in {
      AddressDetails.apply(anInternationalAddress) mustBe AddressDetails(
        line1 = anInternationalAddress.line1,
        line2 = anInternationalAddress.line2,
        line3 = Some(anInternationalAddress.city),
        line4 = anInternationalAddress.region,
        postCode = Some(anInternationalAddress.postal),
        countryCode = Some(anInternationalAddress.country.code)
      )
    }
  }
}
