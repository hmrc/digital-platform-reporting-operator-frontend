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

import models.Country.UnitedKingdom
import models.UkTaxIdentifiers._
import models.operator._
import models.operator.requests.{Notification, UpdatePlatformOperatorRequest}
import models.operator.responses._
import models.{DefaultCountriesList, DueDiligence, InternationalAddress, RegisteredAddressCountry, UkAddress, UkTaxIdentifiers, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import pages.add._
import pages.notification._
import pages.update
import queries.NotificationDetailsQuery

import java.time.Instant

class UserAnswersServiceSpec extends AnyFreeSpec with Matchers with OptionValues with TryValues with EitherValues {

  private val countriesList = new DefaultCountriesList
  private val userAnswersService = new UserAnswersService(countriesList)
  private val emptyAnswers = UserAnswers("id", None)
  private val now = Instant.now

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
            notifications = Seq(
              NotificationDetails(NotificationType.Rpo, Some(true), Some(false), 2024, now),
              NotificationDetails(NotificationType.Epo, None, None, 2025, now)
            )
          )

          val result = userAnswersService.fromPlatformOperator("id", operator).success.value

          result.userId mustEqual "id"
          result.operatorId.value mustEqual "operatorId"

          result.get(BusinessNamePage).value mustEqual "operatorName"
          result.get(HasTradingNamePage).value mustEqual true
          result.get(TradingNamePage).value mustEqual "tradingName"

          result.get(UkTaxIdentifiersPage).value mustEqual Set(Utr, Crn, Vrn, Empref, Chrn)
          result.get(UtrPage).value mustEqual "utr"
          result.get(CrnPage).value mustEqual "crn"
          result.get(VrnPage).value mustEqual "vrn"
          result.get(EmprefPage).value mustEqual "empref"
          result.get(ChrnPage).value mustEqual "chrn"

          result.get(UkAddressPage).value mustEqual UkAddress(
            "line 1",
            Some("line 2"),
            "line 3",
            Some("line 4"),
            "AA1 1AA",
            UnitedKingdom
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

          result.get(NotificationDetailsQuery).value mustEqual Seq(
            NotificationDetails(NotificationType.Rpo, Some(true), Some(false), 2024, now),
            NotificationDetails(NotificationType.Epo, None, None, 2025, now)
          )
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

          result.get(UkTaxIdentifiersPage).value mustEqual Set(Utr)
          result.get(UtrPage).value mustEqual "utr"
          result.get(CrnPage) must not be defined
          result.get(VrnPage) must not be defined
          result.get(EmprefPage) must not be defined
          result.get(ChrnPage) must not be defined

          result.get(UkAddressPage).value mustEqual UkAddress(
            "line 1",
            None,
            "line 3",
            None,
            "AA1 1AA",
            UnitedKingdom
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

          result.get(InternationalAddressPage).value mustEqual InternationalAddress(
            "line 1",
            Some("line 2"),
            "line 3",
            Some("line 4"),
            "zip",
            countriesList.internationalCountries.find(_.code == "US").value
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

          result.get(InternationalAddressPage).value mustEqual InternationalAddress(
            "line 1",
            Some("line 2"),
            "line 3",
            Some("line 4"),
            "zip",
            countriesList.internationalCountries.find(_.code == "US").value
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

        result.get(UkTaxIdentifiersPage) must not be defined
        result.get(UtrPage) must not be defined
        result.get(CrnPage) must not be defined
        result.get(VrnPage) must not be defined
        result.get(EmprefPage) must not be defined
        result.get(ChrnPage) must not be defined
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

        result.get(UkTaxIdentifiersPage) must not be defined
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

        result.get(UkAddressPage) must not be defined
      }
    }
  }

  "addNotificationRequest" - {

    "must create a request" - {

      "for an RPO with extended and active seller DD" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
            .set(UtrPage, "utr").success.value
            .set(CrnPage, "crn").success.value
            .set(VrnPage, "vrn").success.value
            .set(EmprefPage, "empref").success.value
            .set(ChrnPage, "chrn").success.value
            .set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", countriesList.crownDependantCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value
            .set(NotificationTypePage, models.NotificationType.Rpo).success.value
            .set(ReportingPeriodPage, 2024).success.value
            .set(DueDiligencePage, Set[DueDiligence](DueDiligence.Extended, DueDiligence.ActiveSeller)).success.value

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
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(countriesList.crownDependantCountries.head.code)),
          notification = Some(Notification(NotificationType.Rpo, Some(true), Some(true), 2024))
        )

        val result = userAnswersService.addNotificationRequest(answers, "dprsId", "operatorId")

        result.value.copy(tinDetails = result.value.tinDetails.sortBy(_.tin)) mustEqual expectedResult
      }

      "for an RPO with no due diligence" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
            .set(UtrPage, "utr").success.value
            .set(CrnPage, "crn").success.value
            .set(VrnPage, "vrn").success.value
            .set(EmprefPage, "empref").success.value
            .set(ChrnPage, "chrn").success.value
            .set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", countriesList.crownDependantCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value
            .set(NotificationTypePage, models.NotificationType.Rpo).success.value
            .set(ReportingPeriodPage, 2024).success.value
            .set(DueDiligencePage, Set[DueDiligence](DueDiligence.NoDueDiligence)).success.value

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
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(countriesList.crownDependantCountries.head.code)),
          notification = Some(Notification(NotificationType.Rpo, Some(false), Some(false), 2024))
        )

        val result = userAnswersService.addNotificationRequest(answers, "dprsId", "operatorId")

        result.value.copy(tinDetails = result.value.tinDetails.sortBy(_.tin)) mustEqual expectedResult
      }

      "for an EPO" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
            .set(UtrPage, "utr").success.value
            .set(CrnPage, "crn").success.value
            .set(VrnPage, "vrn").success.value
            .set(EmprefPage, "empref").success.value
            .set(ChrnPage, "chrn").success.value
            .set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", countriesList.crownDependantCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value
            .set(NotificationTypePage, models.NotificationType.Epo).success.value
            .set(ReportingPeriodPage, 2024).success.value

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
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(countriesList.crownDependantCountries.head.code)),
          notification = Some(Notification(NotificationType.Epo, None, None, 2024))
        )

        val result = userAnswersService.addNotificationRequest(answers, "dprsId", "operatorId")

        result.value.copy(tinDetails = result.value.tinDetails.sortBy(_.tin)) mustEqual expectedResult
      }
    }

    "must fail to create a request" - {
      "when mandatory fields are missing" in {
        val result = userAnswersService.addNotificationRequest(emptyAnswers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain theSameElementsAs Seq(
          update.BusinessNamePage,
          update.HasTradingNamePage,
          update.UkTaxIdentifiersPage,
          update.PrimaryContactNamePage,
          update.PrimaryContactEmailPage,
          update.CanPhonePrimaryContactPage,
          update.HasSecondaryContactPage,
          update.RegisteredInUkPage,
          NotificationTypePage
        )
      }

      "when the operator is an RPO but due diligence is missing" in {

        val answers =
          emptyAnswers
            .set(BusinessNamePage, "name").success.value
            .set(HasTradingNamePage, false).success.value
            .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
            .set(UtrPage, "utr").success.value
            .set(CrnPage, "crn").success.value
            .set(VrnPage, "vrn").success.value
            .set(EmprefPage, "empref").success.value
            .set(ChrnPage, "chrn").success.value
            .set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", countriesList.crownDependantCountries.head)).success.value
            .set(PrimaryContactNamePage, "contact 1").success.value
            .set(PrimaryContactEmailPage, "contact1@example.com").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value
            .set(NotificationTypePage, models.NotificationType.Rpo).success.value
            .set(ReportingPeriodPage, 2024).success.value

        val result = userAnswersService.addNotificationRequest(answers, "dprsId", "operatorId")

        result.left.value.toChain.toList must contain only DueDiligencePage
      }
    }
  }
}
