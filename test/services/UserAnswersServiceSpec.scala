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

package services

import models.requests.operator.{AddressDetails, ContactDetails, TinDetails, TinType}
import models.requests.operator.requests.CreatePlatformOperatorRequest
import models.{Country, InternationalAddress, UkAddress, UkTaxIdentifiers, UserAnswers}
import org.scalatest.{EitherValues, OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.add._

class UserAnswersServiceSpec extends AnyFreeSpec with Matchers with OptionValues with TryValues with EitherValues {

  private val userAnswersService = new UserAnswersService()

  "toCreatePlatformOperator" - {

    val emptyAnswers = UserAnswers("id", None)

    "must create a request" - {

      "with UK TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, true).success.value
            .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
            .set(UtrPage, "utr").success.value
            .set(CrnPage, "crn").success.value
            .set(VrnPage, "vrn").success.value
            .set(EmprefPage, "empref").success.value
            .set(ChrnPage, "chrn").success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val expectedResult = CreatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorName = "name",
          tinDetails = Seq(
            TinDetails("utr", TinType.Utr, "GB"),
            TinDetails("crn", TinType.Crn, "GB"),
            TinDetails("vrn", TinType.Vrn, "GB"),
            TinDetails("empref", TinType.Empref, "GB"),
            TinDetails("chrn", TinType.Chrn, "GB")
          ),
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "contact 1", "contact1@example.com"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(Country.ukCountries.head.code))
        )

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.value mustEqual expectedResult
      }

      "with international TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, true).success.value
            .set(TradingNamePage, "trading name").success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
            .set(HasInternationalTaxIdentifierPage, true).success.value
            .set(InternationalTaxIdentifierPage, "tax id").success.value
            .set(RegisteredInUkPage, false).success.value
            .set(InternationalAddressPage, InternationalAddress("line 1", None, "town", None, Some("postcode"), Country.internationalCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, true).success.value
            .set(PrimaryContactPhoneNumberPage, "07777 777777").success.value
            .set(HasSecondaryContactPage, true).success.value
            .set(SecondaryContactNamePage, "contact 2").success.value
            .set(SecondaryContactEmailPage, "contact2@example.com").success.value
            .set(CanPhoneSecondaryContactPage, false).success.value

        val expectedResult = CreatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorName = "name",
          tinDetails = Seq(
            TinDetails("tax id", TinType.Other, Country.internationalCountries.head.code)
          ),
          businessName = None,
          tradingName = Some("trading name"),
          primaryContactDetails = ContactDetails(Some("07777 777777"), "contact 1", "contact1@example.com"),
          secondaryContactDetails = Some(ContactDetails(None, "contact 2", "contact2@example.com")),
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("postcode"), Some(Country.internationalCountries.head.code))
        )

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.value mustEqual expectedResult
      }

      "with no UK TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, true).success.value
            .set(SecondaryContactNamePage, "contact 2").success.value
            .set(SecondaryContactEmailPage, "contact2@example.com").success.value
            .set(CanPhoneSecondaryContactPage, true).success.value
            .set(SecondaryContactPhoneNumberPage, "07777 888888").success.value

        val expectedResult = CreatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorName = "name",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "contact 1", "contact1@example.com"),
          secondaryContactDetails = Some(ContactDetails(Some("07777 888888"), "contact 2", "contact2@example.com")),
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(Country.ukCountries.head.code))
        )

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.value mustEqual expectedResult
      }

      "with no international TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, true).success.value
            .set(TradingNamePage, "trading name").success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
            .set(HasInternationalTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, false).success.value
            .set(InternationalAddressPage, InternationalAddress("line 1", None, "town", None, Some("postcode"), Country.internationalCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, true).success.value
            .set(PrimaryContactPhoneNumberPage, "07777 777777").success.value
            .set(HasSecondaryContactPage, true).success.value
            .set(SecondaryContactNamePage, "contact 2").success.value
            .set(SecondaryContactEmailPage, "contact2@example.com").success.value
            .set(CanPhoneSecondaryContactPage, false).success.value

        val expectedResult = CreatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorName = "name",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = Some("trading name"),
          primaryContactDetails = ContactDetails(Some("07777 777777"), "contact 1", "contact1@example.com"),
          secondaryContactDetails = Some(ContactDetails(None, "contact 2", "contact2@example.com")),
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("postcode"), Some(Country.internationalCountries.head.code))
        )

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.value mustEqual expectedResult
      }
    }

    "must fail to create a request" - {

      "when mandatory fields are missing" in {

        val result = userAnswersService.toCreatePlatformOperatorRequest(emptyAnswers, "dprsId")

        result.left.value.toChain.toList must contain theSameElementsAs Seq(
          BusinessNamePage,
          HasTradingNamePage,
          TaxResidentInUkPage,
          PrimaryContactNamePage,
          PrimaryContactEmailPage,
          CanPhonePrimaryContactPage,
          HasSecondaryContactPage,
          RegisteredInUkPage
        )
      }

      "when the operator is UK tax resident but tax identifier information is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only HasUkTaxIdentifierPage
      }

      "when the operator is UK tax resident but tax identifiers are missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, true).success.value
            .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain theSameElementsAs Seq(
          UtrPage, CrnPage, VrnPage, EmprefPage, ChrnPage
        )
      }

      "when the operator is not UK tax resident but tax identifier information is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only HasInternationalTaxIdentifierPage
      }

      "when the registered address is in the UK but the address itself is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only UkAddressPage
      }

      "when the registered address is not in the UK but the address itself is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, false).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only InternationalAddressPage
      }

      "when trading name is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, true).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only TradingNamePage
      }

      "when we can phone the primary contact but their number is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, true).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only PrimaryContactPhoneNumberPage
      }

      "when there is a second contact but their details are missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, true).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain theSameElementsAs Seq(
          SecondaryContactNamePage,
          SecondaryContactEmailPage,
          CanPhoneSecondaryContactPage
        )
      }

      "when we can phone the second contact but their number is missing" in {


        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, true).success.value
            .set(SecondaryContactNamePage, "contact 2").success.value
            .set(SecondaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhoneSecondaryContactPage, true).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only SecondaryContactPhoneNumberPage
      }
    }
  }
}
