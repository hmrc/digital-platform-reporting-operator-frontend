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

import models.UkTaxIdentifiers._
import models.operator._
import models.operator.requests.{CreatePlatformOperatorRequest, UpdatePlatformOperatorRequest}
import models.operator.responses._
import models.{Country, InternationalAddress, UkAddress, UkTaxIdentifiers, UserAnswers}
import org.scalatest.{EitherValues, OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.add._

class UserAnswersServiceSpec extends AnyFreeSpec with Matchers with OptionValues with TryValues with EitherValues {

  private val userAnswersService = new UserAnswersService()

  "fromPlatformOperator" - {

    "must return a UserAnswers populated from the platform operator" - {

      "for a UK operator" - {

        "when optional fields are present" in {

          val operator = PlatformOperator(
            operatorId = "operatorId",
            operatorName = "operatorName",
            tinDetails = Seq(
              TinDetails("utr", TinType.Utr, "GB"),
              TinDetails("crn", TinType.Crn, "GB"),
              TinDetails("vrn", TinType.Vrn, "GB"),
              TinDetails("empref", TinType.Empref, "GB"),
              TinDetails("chrn", TinType.Chrn, "GB")
            ),
            businessName = Some("businessName"),
            tradingName = Some("tradingName"),
            primaryContactDetails = ContactDetails(Some("primaryPhone"), "primaryName", "primaryEmail"),
            secondaryContactDetails = Some(ContactDetails(Some("secondaryPhone"), "secondaryName", "secondaryEmail")),
            addressDetails = AddressDetails("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("AA1 1AA"), Some("GB")),
            notifications = Seq.empty
          )

          val result = userAnswersService.fromPlatformOperator("id", operator).success.value

          result.userId mustEqual "id"
          result.operatorId.value mustEqual "operatorId"

          result.get(BusinessNamePage).value mustEqual "operatorName"
          result.get(HasTradingNamePage).value mustEqual true
          result.get(TradingNamePage).value mustEqual "tradingName"

          result.get(HasTaxIdentifierPage).value mustEqual true
          result.get(TaxResidentInUkPage).value mustEqual true
          result.get(UkTaxIdentifiersPage).value mustEqual Set(Utr, Crn, Vrn, Empref, Chrn)
          result.get(UtrPage).value mustEqual "utr"
          result.get(CrnPage).value mustEqual "crn"
          result.get(VrnPage).value mustEqual "vrn"
          result.get(EmprefPage).value mustEqual "empref"
          result.get(ChrnPage).value mustEqual "chrn"

          result.get(RegisteredInUkPage).value mustEqual true
          result.get(UkAddressPage).value mustEqual UkAddress(
            "line 1",
            Some("line 2"),
            "line 3",
            Some("line 4"),
            "AA1 1AA",
            Country.ukCountries.find(_.code == "GB").value
          )

          result.get(PrimaryContactNamePage).value mustEqual "primaryName"
          result.get(PrimaryContactEmailPage).value mustEqual "primaryEmail"
          result.get(CanPhonePrimaryContactPage).value mustBe true
          result.get(PrimaryContactPhoneNumberPage).value mustBe "primaryPhone"

          result.get(HasSecondaryContactPage).value mustBe true
          result.get(SecondaryContactNamePage).value mustEqual "secondaryName"
          result.get(SecondaryContactEmailPage).value mustEqual "secondaryEmail"
          result.get(CanPhoneSecondaryContactPage).value mustBe true
          result.get(SecondaryContactPhoneNumberPage).value mustBe "secondaryPhone"
        }

        "when optional fields are not present" in {

          val operator = PlatformOperator(
            operatorId = "operatorId",
            operatorName = "operatorName",
            tinDetails = Seq(TinDetails("utr", TinType.Utr, "GB")),
            businessName = None,
            tradingName = None,
            primaryContactDetails = ContactDetails(None, "primaryName", "primaryEmail"),
            secondaryContactDetails = None,
            addressDetails = AddressDetails("line 1", None, Some("line 3"), None, Some("AA1 1AA"), Some("GB")),
            notifications = Seq.empty
          )

          val result = userAnswersService.fromPlatformOperator("id", operator).success.value

          result.userId mustEqual "id"
          result.operatorId.value mustEqual "operatorId"

          result.get(BusinessNamePage).value mustEqual "operatorName"
          result.get(HasTradingNamePage).value mustEqual false
          result.get(TradingNamePage) must not be defined

          result.get(HasTaxIdentifierPage).value mustEqual true
          result.get(TaxResidentInUkPage).value mustEqual true
          result.get(UkTaxIdentifiersPage).value mustEqual Set(Utr)
          result.get(UtrPage).value mustEqual "utr"
          result.get(CrnPage) must not be defined
          result.get(VrnPage) must not be defined
          result.get(EmprefPage) must not be defined
          result.get(ChrnPage) must not be defined

          result.get(RegisteredInUkPage).value mustEqual true
          result.get(UkAddressPage).value mustEqual UkAddress(
            "line 1",
            None,
            "line 3",
            None,
            "AA1 1AA",
            Country.ukCountries.find(_.code == "GB").value
          )

          result.get(PrimaryContactNamePage).value mustEqual "primaryName"
          result.get(PrimaryContactEmailPage).value mustEqual "primaryEmail"
          result.get(CanPhonePrimaryContactPage).value mustBe false

          result.get(HasSecondaryContactPage).value mustBe false
        }

        "when there are some non-UK TIN details (should never happen but technically possible)" in {

          val operator = PlatformOperator(
            operatorId = "operatorId",
            operatorName = "operatorName",
            tinDetails = Seq(
              TinDetails("other", TinType.Other, "US"),
              TinDetails("utr", TinType.Utr, "GB")
            ),
            businessName = None,
            tradingName = None,
            primaryContactDetails = ContactDetails(None, "primaryName", "primaryEmail"),
            secondaryContactDetails = None,
            addressDetails = AddressDetails("line 1", None, Some("line 3"), None, Some("AA1 1AA"), Some("GB")),
            notifications = Seq.empty
          )

          val result = userAnswersService.fromPlatformOperator("id", operator).success.value

          result.get(TaxResidentInUkPage).value mustEqual true
          result.get(HasTaxIdentifierPage).value mustEqual true
          result.get(UkTaxIdentifiersPage).value mustEqual Set(Utr)
          result.get(UtrPage).value mustEqual "utr"
          result.get(CrnPage) must not be defined
          result.get(VrnPage) must not be defined
          result.get(EmprefPage) must not be defined
          result.get(ChrnPage) must not be defined
        }

        "when there is a single UK `OTHER` TIN (should never happen but technically possible)" in {

          val operator = PlatformOperator(
            operatorId = "operatorId",
            operatorName = "operatorName",
            tinDetails = Seq(
              TinDetails("other", TinType.Other, "GB")
            ),
            businessName = None,
            tradingName = None,
            primaryContactDetails = ContactDetails(None, "primaryName", "primaryEmail"),
            secondaryContactDetails = None,
            addressDetails = AddressDetails("line 1", None, Some("line 3"), None, Some("AA1 1AA"), Some("GB")),
            notifications = Seq.empty
          )

          val result = userAnswersService.fromPlatformOperator("id", operator).success.value

          result.get(HasTaxIdentifierPage).value mustEqual true
          result.get(TaxResidentInUkPage).value mustEqual true
          result.get(UkTaxIdentifiersPage) must not be defined
          result.get(UtrPage) must not be defined
          result.get(CrnPage) must not be defined
          result.get(VrnPage) must not be defined
          result.get(EmprefPage) must not be defined
          result.get(ChrnPage) must not be defined
        }
      }

      "for an international operator" - {

        "when optional fields are present" in {

          val operator = PlatformOperator(
            operatorId = "operatorId",
            operatorName = "operatorName",
            tinDetails = Seq(TinDetails("other", TinType.Other, "US")),
            businessName = Some("businessName"),
            tradingName = Some("tradingName"),
            primaryContactDetails = ContactDetails(Some("primaryPhone"), "primaryName", "primaryEmail"),
            secondaryContactDetails = Some(ContactDetails(Some("secondaryPhone"), "secondaryName", "secondaryEmail")),
            addressDetails = AddressDetails("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("zip"), Some("US")),
            notifications = Seq.empty
          )

          val result = userAnswersService.fromPlatformOperator("id", operator).success.value

          result.userId mustEqual "id"
          result.operatorId.value mustEqual "operatorId"

          result.get(BusinessNamePage).value mustEqual "operatorName"
          result.get(HasTradingNamePage).value mustEqual true
          result.get(TradingNamePage).value mustEqual "tradingName"

          result.get(HasTaxIdentifierPage).value mustEqual true
          result.get(TaxResidentInUkPage).value mustEqual false
          result.get(InternationalTaxIdentifierPage).value mustEqual "other"

          result.get(RegisteredInUkPage).value mustEqual false
          result.get(InternationalAddressPage).value mustEqual InternationalAddress(
            "line 1",
            Some("line 2"),
            "line 3",
            Some("line 4"),
            "zip",
            Country.internationalCountries.find(_.code == "US").value
          )

          result.get(PrimaryContactNamePage).value mustEqual "primaryName"
          result.get(PrimaryContactEmailPage).value mustEqual "primaryEmail"
          result.get(CanPhonePrimaryContactPage).value mustBe true
          result.get(PrimaryContactPhoneNumberPage).value mustBe "primaryPhone"

          result.get(HasSecondaryContactPage).value mustBe true
          result.get(SecondaryContactNamePage).value mustEqual "secondaryName"
          result.get(SecondaryContactEmailPage).value mustEqual "secondaryEmail"
          result.get(CanPhoneSecondaryContactPage).value mustBe true
          result.get(SecondaryContactPhoneNumberPage).value mustBe "secondaryPhone"
        }

        "when there is more than one international TIN (should never happen but technically possible)" in {

          val operator = PlatformOperator(
            operatorId = "operatorId",
            operatorName = "operatorName",
            tinDetails = Seq(
              TinDetails("other", TinType.Other, "US"),
              TinDetails("foo", TinType.Other, "FR")
            ),
            businessName = Some("businessName"),
            tradingName = Some("tradingName"),
            primaryContactDetails = ContactDetails(Some("primaryPhone"), "primaryName", "primaryEmail"),
            secondaryContactDetails = Some(ContactDetails(Some("secondaryPhone"), "secondaryName", "secondaryEmail")),
            addressDetails = AddressDetails("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("zip"), Some("US")),
            notifications = Seq.empty
          )

          val result = userAnswersService.fromPlatformOperator("id", operator).success.value

          result.userId mustEqual "id"
          result.operatorId.value mustEqual "operatorId"

          result.get(BusinessNamePage).value mustEqual "operatorName"
          result.get(HasTradingNamePage).value mustEqual true
          result.get(TradingNamePage).value mustEqual "tradingName"

          result.get(HasTaxIdentifierPage).value mustEqual true
          result.get(TaxResidentInUkPage).value mustEqual false
          result.get(InternationalTaxIdentifierPage).value mustEqual "other"

          result.get(RegisteredInUkPage).value mustEqual false
          result.get(InternationalAddressPage).value mustEqual InternationalAddress(
            "line 1",
            Some("line 2"),
            "line 3",
            Some("line 4"),
            "zip",
            Country.internationalCountries.find(_.code == "US").value
          )

          result.get(PrimaryContactNamePage).value mustEqual "primaryName"
          result.get(PrimaryContactEmailPage).value mustEqual "primaryEmail"
          result.get(CanPhonePrimaryContactPage).value mustBe true
          result.get(PrimaryContactPhoneNumberPage).value mustBe "primaryPhone"

          result.get(HasSecondaryContactPage).value mustBe true
          result.get(SecondaryContactNamePage).value mustEqual "secondaryName"
          result.get(SecondaryContactEmailPage).value mustEqual "secondaryEmail"
          result.get(CanPhoneSecondaryContactPage).value mustBe true
          result.get(SecondaryContactPhoneNumberPage).value mustBe "secondaryPhone"
        }
      }

      "when there are no TIN details" in {

        val operator = PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Seq.empty,
          businessName = Some("businessName"),
          tradingName = Some("tradingName"),
          primaryContactDetails = ContactDetails(Some("primaryPhone"), "primaryName", "primaryEmail"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("AA1 1AA"), Some("GB")),
          notifications = Seq.empty
        )

        val result = userAnswersService.fromPlatformOperator("id", operator).success.value

        result.get(HasTaxIdentifierPage).value mustEqual false
        result.get(TaxResidentInUkPage) must not be defined
        result.get(UkTaxIdentifiersPage) must not be defined
        result.get(UtrPage) must not be defined
        result.get(CrnPage) must not be defined
        result.get(VrnPage) must not be defined
        result.get(EmprefPage) must not be defined
        result.get(ChrnPage) must not be defined
        result.get(TaxResidencyCountryPage) must not be defined
        result.get(InternationalTaxIdentifierPage) must not be defined
      }

      "when TIN details contain a country code that is not recognised" in {

        val operator = PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Seq(TinDetails("other", TinType.Other, "Not a country code")),
          businessName = Some("businessName"),
          tradingName = Some("tradingName"),
          primaryContactDetails = ContactDetails(Some("primaryPhone"), "primaryName", "primaryEmail"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("zip"), Some("US")),
          notifications = Seq.empty
        )

        val result = userAnswersService.fromPlatformOperator("id", operator).success.value

        result.get(HasTaxIdentifierPage).value mustEqual false
        result.get(TaxResidentInUkPage) must not be defined
        result.get(TaxResidencyCountryPage) must not be defined
        result.get(UkTaxIdentifiersPage) must not be defined
        result.get(InternationalTaxIdentifierPage) must not be defined
      }

      "when the registered address contains a country code that is not recognised, so we cannot infer the registered address" in {

        val operator = PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Seq(TinDetails("other", TinType.Other, "US")),
          businessName = Some("businessName"),
          tradingName = Some("tradingName"),
          primaryContactDetails = ContactDetails(Some("primaryPhone"), "primaryName", "primaryEmail"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("zip"), Some("Not a country code")),
          notifications = Seq.empty
        )

        val result = userAnswersService.fromPlatformOperator("id", operator).success.value

        result.get(RegisteredInUkPage) must not be defined
        result.get(UkAddressPage) must not be defined
        result.get(InternationalAddressPage) must not be defined
      }

      "when a UK address does not contain mandatory data and therefore cannot be built" in {

        val operator = PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Seq(TinDetails("other", TinType.Other, "Not a country code")),
          businessName = Some("businessName"),
          tradingName = Some("tradingName"),
          primaryContactDetails = ContactDetails(Some("primaryPhone"), "primaryName", "primaryEmail"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, Some("AA1 1AA"), Some("GB")),
          notifications = Seq.empty
        )

        val result = userAnswersService.fromPlatformOperator("id", operator).success.value

        result.get(RegisteredInUkPage) must not be defined
        result.get(UkAddressPage) must not be defined
      }

      "when an international address does not contain mandatory data and therefore cannot be built" in {

        val operator = PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Seq(TinDetails("other", TinType.Other, "Not a country code")),
          businessName = Some("businessName"),
          tradingName = Some("tradingName"),
          primaryContactDetails = ContactDetails(Some("primaryPhone"), "primaryName", "primaryEmail"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, Some("AA1 1AA"), Some("US")),
          notifications = Seq.empty
        )

        val result = userAnswersService.fromPlatformOperator("id", operator).success.value

        result.get(RegisteredInUkPage) must not be defined
        result.get(UkAddressPage) must not be defined
      }
    }
  }

  "toCreatePlatformOperator" - {

    val emptyAnswers = UserAnswers("id", None)

    "must create a request" - {

      "with UK TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, true).success.value
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
            TinDetails("chrn", TinType.Chrn, "GB"),
            TinDetails("crn", TinType.Crn, "GB"),
            TinDetails("empref", TinType.Empref, "GB"),
            TinDetails("utr", TinType.Utr, "GB"),
            TinDetails("vrn", TinType.Vrn, "GB")
          ),
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "contact 1", "contact1@example.com"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(Country.ukCountries.head.code))
        )

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.value.copy(tinDetails = result.value.tinDetails.sortBy(_.tin)) mustEqual expectedResult
      }

      "with international TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, true).success.value
            .set(TradingNamePage, "trading name").success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
            .set(InternationalTaxIdentifierPage, "tax id").success.value
            .set(RegisteredInUkPage, false).success.value
            .set(InternationalAddressPage, InternationalAddress("line 1", None, "town", None, "postcode", Country.internationalCountries.head)).success.value
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
            .set(HasTaxIdentifierPage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
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
            .set(HasTaxIdentifierPage, false).success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
            .set(RegisteredInUkPage, false).success.value
            .set(InternationalAddressPage, InternationalAddress("line 1", None, "town", None, "postcode", Country.internationalCountries.head)).success.value
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
          HasTaxIdentifierPage,
          PrimaryContactNamePage,
          PrimaryContactEmailPage,
          CanPhonePrimaryContactPage,
          HasSecondaryContactPage,
          RegisteredInUkPage
        )
      }

      "when the operator has tax identifiers but whether they are tax resident in the UK is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only TaxResidentInUkPage
      }

      "when the operator is UK tax resident but tax identifier information is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain only UkTaxIdentifiersPage
      }

      "when the operator is UK tax resident but tax identifiers are missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, true).success.value
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

      "when the operator is not UK tax resident but country of residency is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toCreatePlatformOperatorRequest(answers, "dprsId")

        result.left.value.toChain.toList must contain theSameElementsAs Seq(
          TaxResidencyCountryPage, InternationalTaxIdentifierPage
        )
      }

      "when the registered address is in the UK but the address itself is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, false).success.value
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
            .set(HasTaxIdentifierPage, false).success.value
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
            .set(HasTaxIdentifierPage, false).success.value
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
            .set(HasTaxIdentifierPage, false).success.value
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
            .set(HasTaxIdentifierPage, false).success.value
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
            .set(HasTaxIdentifierPage, false).success.value
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

  "toUpdatePlatformOperator" - {

    val emptyAnswers = UserAnswers("id", None)

    "must create a request" - {

      "with UK TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, true).success.value
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

        val expectedResult = UpdatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorId = "operatorId",
          operatorName = "name",
          tinDetails = Seq(
            TinDetails("chrn", TinType.Chrn, "GB"),
            TinDetails("crn", TinType.Crn, "GB"),
            TinDetails("empref", TinType.Empref, "GB"),
            TinDetails("utr", TinType.Utr, "GB"),
            TinDetails("vrn", TinType.Vrn, "GB")
          ),
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "contact 1", "contact1@example.com"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(Country.ukCountries.head.code)),
          notification = None
        )

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.value.copy(tinDetails = result.value.tinDetails.sortBy(_.tin)) mustEqual expectedResult
      }

      "with international TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, true).success.value
            .set(TradingNamePage, "trading name").success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
            .set(InternationalTaxIdentifierPage, "tax id").success.value
            .set(RegisteredInUkPage, false).success.value
            .set(InternationalAddressPage, InternationalAddress("line 1", None, "town", None, "postcode", Country.internationalCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, true).success.value
            .set(PrimaryContactPhoneNumberPage, "07777 777777").success.value
            .set(HasSecondaryContactPage, true).success.value
            .set(SecondaryContactNamePage, "contact 2").success.value
            .set(SecondaryContactEmailPage, "contact2@example.com").success.value
            .set(CanPhoneSecondaryContactPage, false).success.value

        val expectedResult = UpdatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorId = "operatorId",
          operatorName = "name",
          tinDetails = Seq(
            TinDetails("tax id", TinType.Other, Country.internationalCountries.head.code)
          ),
          businessName = None,
          tradingName = Some("trading name"),
          primaryContactDetails = ContactDetails(Some("07777 777777"), "contact 1", "contact1@example.com"),
          secondaryContactDetails = Some(ContactDetails(None, "contact 2", "contact2@example.com")),
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("postcode"), Some(Country.internationalCountries.head.code)),
          notification = None
        )

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.value mustEqual expectedResult
      }

      "with no UK TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
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

        val expectedResult = UpdatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorId = "operatorId",
          operatorName = "name",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "contact 1", "contact1@example.com"),
          secondaryContactDetails = Some(ContactDetails(Some("07777 888888"), "contact 2", "contact2@example.com")),
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(Country.ukCountries.head.code)),
          notification = None
        )

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.value mustEqual expectedResult
      }

      "with no international TIN details" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, true).success.value
            .set(TradingNamePage, "trading name").success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
            .set(RegisteredInUkPage, false).success.value
            .set(InternationalAddressPage, InternationalAddress("line 1", None, "town", None, "postcode", Country.internationalCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, true).success.value
            .set(PrimaryContactPhoneNumberPage, "07777 777777").success.value
            .set(HasSecondaryContactPage, true).success.value
            .set(SecondaryContactNamePage, "contact 2").success.value
            .set(SecondaryContactEmailPage, "contact2@example.com").success.value
            .set(CanPhoneSecondaryContactPage, false).success.value

        val expectedResult = UpdatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorId = "operatorId",
          operatorName = "name",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = Some("trading name"),
          primaryContactDetails = ContactDetails(Some("07777 777777"), "contact 1", "contact1@example.com"),
          secondaryContactDetails = Some(ContactDetails(None, "contact 2", "contact2@example.com")),
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("postcode"), Some(Country.internationalCountries.head.code)),
          notification = None
        )

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.value mustEqual expectedResult
      }
    }

    "must fail to create a request" - {

      "when mandatory fields are missing" in {

        val result = userAnswersService.toUpdatePlatformOperatorRequest(emptyAnswers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain theSameElementsAs Seq(
          BusinessNamePage,
          HasTradingNamePage,
          HasTaxIdentifierPage,
          PrimaryContactNamePage,
          PrimaryContactEmailPage,
          CanPhonePrimaryContactPage,
          HasSecondaryContactPage,
          RegisteredInUkPage
        )
      }

      "when the operator has tax identifiers but whether they are tax resident in the UK is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain only TaxResidentInUkPage
      }

      "when the operator is UK tax resident but tax identifier information is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain only UkTaxIdentifiersPage
      }

      "when the operator is UK tax resident but tax identifiers are missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain theSameElementsAs Seq(
          UtrPage, CrnPage, VrnPage, EmprefPage, ChrnPage
        )
      }

      "when the operator is not UK tax resident but country of residency is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, true).success.value
            .set(TaxResidentInUkPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain theSameElementsAs Seq(
          TaxResidencyCountryPage, InternationalTaxIdentifierPage
        )
      }

      "when the registered address is in the UK but the address itself is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain only UkAddressPage
      }

      "when the registered address is not in the UK but the address itself is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, false).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain only InternationalAddressPage
      }

      "when trading name is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, true).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain only TradingNamePage
      }

      "when we can phone the primary contact but their number is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, true).success.value
            .set(HasSecondaryContactPage, false).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain only PrimaryContactPhoneNumberPage
      }

      "when there is a second contact but their details are missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, true).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

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
            .set(HasTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, true).success.value
            .set(SecondaryContactNamePage, "contact 2").success.value
            .set(SecondaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhoneSecondaryContactPage, true).success.value

        val result = userAnswersService.toUpdatePlatformOperatorRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain only SecondaryContactPhoneNumberPage
      }
    }
  }
}
