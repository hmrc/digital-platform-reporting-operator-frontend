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

package models.operator.requests

import builders.InternationalAddressBuilder.anInternationalAddress
import builders.JerseyGuernseyIoMAddressBuilder.aJerseyGuernseyIoMAddress
import builders.UkAddressBuilder.aUkAddress
import builders.UserAnswersBuilder.aUserAnswers
import models.RegisteredAddressCountry.{International, JerseyGuernseyIsleOfMan, Uk}
import models.UkTaxIdentifiers
import models.operator.{AddressDetails, ContactDetails, TinDetails, TinType}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import pages.update._

class UpdatePlatformOperatorRequestSpec extends AnyFreeSpec with Matchers
  with TryValues
  with OptionValues
  with EitherValues {

  private val underTest = UpdatePlatformOperatorRequest

  "getAddressDetails" - {
    "must return correct AddressDetails when corresponding values for" - {
      "RegisteredInUkPage an UkAddressPage" in {
        val answers = aUserAnswers
          .set(RegisteredInUkPage, Uk).success.value
          .set(UkAddressPage, aUkAddress).success.value

        underTest.getAddressDetails(answers) mustBe Right(AddressDetails(aUkAddress))
      }

      "RegisteredInUkPage an JerseyGuernseyIoMAddressPage" in {
        val answers = aUserAnswers
          .set(RegisteredInUkPage, JerseyGuernseyIsleOfMan).success.value
          .set(JerseyGuernseyIoMAddressPage, aJerseyGuernseyIoMAddress).success.value

        underTest.getAddressDetails(answers) mustBe Right(AddressDetails(aJerseyGuernseyIoMAddress))
      }

      "RegisteredInUkPage an InternationalAddressPage" in {
        val answers = aUserAnswers
          .set(RegisteredInUkPage, International).success.value
          .set(InternationalAddressPage, anInternationalAddress).success.value

        underTest.getAddressDetails(answers) mustBe Right(AddressDetails(anInternationalAddress))
      }
    }

    "must return list of errors when" - {
      "RegisteredInUkPage does not exist" in {
        val answers = aUserAnswers.remove(RegisteredInUkPage).success.value
        val result = underTest.getAddressDetails(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(RegisteredInUkPage)
      }

      "RegisteredInUkPage exists, but has no corresponding address" in {
        val answers = aUserAnswers
          .set(RegisteredInUkPage, International).success.value
          .set(UkAddressPage, aUkAddress).success.value
        val result = underTest.getAddressDetails(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(InternationalAddressPage)
      }
    }
  }

  "getSecondaryContactPhone" - {
    "must return None when CanPhoneSecondaryContactPage is false" in {
      val answers = aUserAnswers.set(CanPhoneSecondaryContactPage, false).success.value

      underTest.getSecondaryContactPhone(answers) mustBe Right(None)
    }

    "must return value when CanPhoneSecondaryContactPage is true and SecondaryContactPhoneNumberPage is provided" in {
      val answers = aUserAnswers
        .set(CanPhoneSecondaryContactPage, true).success.value
        .set(SecondaryContactPhoneNumberPage, "some-phone-number").success.value

      underTest.getSecondaryContactPhone(answers) mustBe Right(Some("some-phone-number"))
    }

    "must return list of errors when" - {
      "CanPhoneSecondaryContactPage does not exist" in {
        val answers = aUserAnswers.remove(CanPhoneSecondaryContactPage).success.value
        val result = underTest.getSecondaryContactPhone(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(CanPhoneSecondaryContactPage)
      }

      "CanPhoneSecondaryContactPage is true, but SecondaryContactPhoneNumberPage is missing" in {
        val answers = aUserAnswers
          .set(CanPhoneSecondaryContactPage, true).success.value
          .remove(SecondaryContactPhoneNumberPage).success.value
        val result = underTest.getSecondaryContactPhone(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(SecondaryContactPhoneNumberPage)
      }
    }
  }

  "getSecondaryContact" - {
    "must return ContactDetails when relevant data is available" in {
      val answers = aUserAnswers
        .set(HasSecondaryContactPage, true).success.value
        .set(SecondaryContactNamePage, "some-secondary-contact-name").success.value
        .set(SecondaryContactEmailPage, "some-secondary-contact@example.com").success.value
        .set(CanPhoneSecondaryContactPage, true).success.value
        .set(SecondaryContactPhoneNumberPage, "some-phone-number").success.value

      underTest.getSecondaryContact(answers) mustBe Right(Some(ContactDetails(
        phoneNumber = Some("some-phone-number"),
        contactName = "some-secondary-contact-name",
        emailAddress = "some-secondary-contact@example.com"
      )))
    }

    "must return None when HasSecondaryContactPage is false" in {
      val answers = aUserAnswers.set(HasSecondaryContactPage, false).success.value
      underTest.getSecondaryContact(answers) mustBe Right(None)
    }

    "must return a list of relevant errors when data not provided" in {
      val answers = aUserAnswers
        .set(HasSecondaryContactPage, true).success.value
        .remove(SecondaryContactNamePage).success.value
        .remove(SecondaryContactEmailPage).success.value
        .remove(CanPhoneSecondaryContactPage).success.value
      val result = underTest.getSecondaryContact(answers)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(
        SecondaryContactNamePage,
        SecondaryContactEmailPage,
        CanPhoneSecondaryContactPage
      )
    }
  }

  "getPrimaryContactPhone" - {
    "must return phone number when relevant data is available" in {
      val answers = aUserAnswers
        .set(CanPhonePrimaryContactPage, true).success.value
        .set(PrimaryContactPhoneNumberPage, "some-phone-number").success.value

      underTest.getPrimaryContactPhone(answers) mustBe Right(Some("some-phone-number"))
    }

    "must return None when CanPhonePrimaryContactPage is false" in {
      val answers = aUserAnswers.set(CanPhonePrimaryContactPage, false).success.value
      underTest.getPrimaryContactPhone(answers) mustBe Right(None)
    }

    "must return a list of relevant errors when" - {
      "CanPhonePrimaryContactPage not provided" in {
        val answers = aUserAnswers.remove(CanPhonePrimaryContactPage).success.value
        val result = underTest.getPrimaryContactPhone(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(CanPhonePrimaryContactPage)
      }

      "CanPhonePrimaryContactPage is provided and PrimaryContactPhoneNumberPage not provided" in {
        val answers = aUserAnswers
          .set(CanPhonePrimaryContactPage, true).success.value
          .remove(PrimaryContactPhoneNumberPage).success.value
        val result = underTest.getPrimaryContactPhone(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(PrimaryContactPhoneNumberPage)
      }
    }
  }

  "getPrimaryContact" - {
    "must return ContactDetails when relevant data is available" in {
      val answers = aUserAnswers
        .set(PrimaryContactNamePage, "some-primary-contact-name").success.value
        .set(PrimaryContactEmailPage, "some-primary-contact@example.com").success.value
        .set(CanPhonePrimaryContactPage, true).success.value
        .set(PrimaryContactPhoneNumberPage, "some-phone-number").success.value

      underTest.getPrimaryContact(answers) mustBe Right(ContactDetails(
        phoneNumber = Some("some-phone-number"),
        contactName = "some-primary-contact-name",
        emailAddress = "some-primary-contact@example.com"
      ))
    }

    "must return a list of relevant errors when data not provided" in {
      val answers = aUserAnswers
        .remove(PrimaryContactNamePage).success.value
        .remove(PrimaryContactEmailPage).success.value
        .remove(CanPhonePrimaryContactPage).success.value
      val result = underTest.getPrimaryContact(answers)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(
        PrimaryContactNamePage,
        PrimaryContactEmailPage,
        CanPhonePrimaryContactPage
      )
    }
  }

  "getUkTin" - {
    "given Utr identifier" - {
      "must return correct TinDetails when data available" in {
        val answers = aUserAnswers.set(UtrPage, "some-tax-identifier").success.value
        underTest.getUkTin(UkTaxIdentifiers.Utr, answers) mustBe Right(TinDetails(
          tin = "some-tax-identifier",
          tinType = TinType.Utr,
          issuedBy = "GB"
        ))
      }

      "must return error data not available" in {
        val answers = aUserAnswers.remove(UtrPage).success.value
        val result = underTest.getUkTin(UkTaxIdentifiers.Utr, answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(UtrPage)
      }
    }

    "given Crn identifier" - {
      "must return correct TinDetails when data available" in {
        val answers = aUserAnswers.set(CrnPage, "some-tax-identifier").success.value
        underTest.getUkTin(UkTaxIdentifiers.Crn, answers) mustBe Right(TinDetails(
          tin = "some-tax-identifier",
          tinType = TinType.Crn,
          issuedBy = "GB"
        ))
      }

      "must return error data not available" in {
        val answers = aUserAnswers.remove(CrnPage).success.value
        val result = underTest.getUkTin(UkTaxIdentifiers.Crn, answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(CrnPage)
      }
    }

    "given Vrn identifier" - {
      "must return correct TinDetails when data available" in {
        val answers = aUserAnswers.set(VrnPage, "some-tax-identifier").success.value
        underTest.getUkTin(UkTaxIdentifiers.Vrn, answers) mustBe Right(TinDetails(
          tin = "some-tax-identifier",
          tinType = TinType.Vrn,
          issuedBy = "GB"
        ))
      }

      "must return error data not available" in {
        val answers = aUserAnswers.remove(VrnPage).success.value
        val result = underTest.getUkTin(UkTaxIdentifiers.Vrn, answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(VrnPage)
      }
    }

    "given Empref identifier" - {
      "must return correct TinDetails when data available" in {
        val answers = aUserAnswers.set(EmprefPage, "some-tax-identifier").success.value
        underTest.getUkTin(UkTaxIdentifiers.Empref, answers) mustBe Right(TinDetails(
          tin = "some-tax-identifier",
          tinType = TinType.Empref,
          issuedBy = "GB"
        ))
      }

      "must return error data not available" in {
        val answers = aUserAnswers.remove(EmprefPage).success.value
        val result = underTest.getUkTin(UkTaxIdentifiers.Empref, answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(EmprefPage)
      }
    }

    "given Chrn identifier" - {
      "must return correct TinDetails when data available" in {
        val answers = aUserAnswers.set(ChrnPage, "some-tax-identifier").success.value
        underTest.getUkTin(UkTaxIdentifiers.Chrn, answers) mustBe Right(TinDetails(
          tin = "some-tax-identifier",
          tinType = TinType.Chrn,
          issuedBy = "GB"
        ))
      }

      "must return error data not available" in {
        val answers = aUserAnswers.remove(ChrnPage).success.value
        val result = underTest.getUkTin(UkTaxIdentifiers.Chrn, answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(ChrnPage)
      }
    }
  }

  "getUkTinDetails" - {
    "must return TinDetails list when relevant data is available" in {
      val names: Set[UkTaxIdentifiers] = Set(UkTaxIdentifiers.Utr, UkTaxIdentifiers.Crn)
      val answers = aUserAnswers
        .set(UkTaxIdentifiersPage, names).success.value
        .set(UtrPage, "some-utr").success.value
        .set(CrnPage, "some-crn").success.value

      underTest.getUkTinDetails(answers) mustBe Right(Seq(
        TinDetails("some-utr", TinType.Utr, "GB"),
        TinDetails("some-crn", TinType.Crn, "GB")
      ))
    }

    "must return error when data not available" in {
      val answers = aUserAnswers.remove(UkTaxIdentifiersPage).success.value
      val result = underTest.getUkTinDetails(answers)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(UkTaxIdentifiersPage)
    }
  }

  "getTradingName" - {
    "must return None when HasTradingNamePage is false" in {
      val answers = aUserAnswers.set(HasTradingNamePage, false).success.value
      underTest.getTradingName(answers) mustBe Right(None)
    }

    "must return value when HasTradingNamePage is true and TradingNamePage is provided" in {
      val answers = aUserAnswers
        .set(HasTradingNamePage, true).success.value
        .set(TradingNamePage, "some-trading-name").success.value
      underTest.getTradingName(answers) mustBe Right(Some("some-trading-name"))
    }

    "must return list of errors when" - {
      "HasTradingNamePage does not exist" in {
        val answers = aUserAnswers.remove(HasTradingNamePage).success.value
        val result = underTest.getTradingName(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(HasTradingNamePage)
      }

      "HasTradingNamePage is true, but TradingNamePage is missing" in {
        val answers = aUserAnswers
          .set(HasTradingNamePage, true).success.value
          .remove(TradingNamePage).success.value
        val result = underTest.getTradingName(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(TradingNamePage)
      }
    }
  }

  ".build" - {
    "must create correct CreatePlatformOperatorRequest when all data provided" in {
      val names: Set[UkTaxIdentifiers] = Set(UkTaxIdentifiers.Utr)
      val answers = aUserAnswers
        .set(BusinessNamePage, "some-business-name").success.value
        .set(HasTradingNamePage, true).success.value
        .set(TradingNamePage, "some-trading-name").success.value
        .set(UkTaxIdentifiersPage, names).success.value
        .set(UtrPage, "some-utr").success.value
        .set(PrimaryContactNamePage, "contact-name").success.value
        .set(PrimaryContactEmailPage, "contact-email@example.com").success.value
        .set(CanPhonePrimaryContactPage, true).success.value
        .set(PrimaryContactPhoneNumberPage, "some-phone-number").success.value
        .set(HasSecondaryContactPage, true).success.value
        .set(SecondaryContactNamePage, "secondary-contact-name").success.value
        .set(SecondaryContactEmailPage, "secondary-contact-name@example.com").success.value
        .set(CanPhoneSecondaryContactPage, true).success.value
        .set(SecondaryContactPhoneNumberPage, "some-secondary-phone-number").success.value
        .set(RegisteredInUkPage, Uk).success.value
        .set(UkAddressPage, aUkAddress).success.value

      underTest.build(answers, "some-dprs-id", "some-operator-id") mustBe Right(UpdatePlatformOperatorRequest(
        subscriptionId = "some-dprs-id",
        operatorId = "some-operator-id",
        operatorName = "some-business-name",
        tinDetails = Seq(TinDetails("some-utr", TinType.Utr, "GB")),
        businessName = None,
        tradingName = Some("some-trading-name"),
        primaryContactDetails = ContactDetails(Some("some-phone-number"), "contact-name", "contact-email@example.com"),
        secondaryContactDetails = Some(ContactDetails(Some("some-secondary-phone-number"), "secondary-contact-name", "secondary-contact-name@example.com")),
        addressDetails = AddressDetails(aUkAddress),
        notification = None
      ))
    }

    "must return a list of errors when not all data provided" in {
      val answers = aUserAnswers
        .remove(BusinessNamePage).success.value
        .remove(HasTradingNamePage).success.value
        .remove(UkTaxIdentifiersPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(PrimaryContactEmailPage).success.value
        .remove(CanPhonePrimaryContactPage).success.value
        .remove(HasSecondaryContactPage).success.value
        .remove(RegisteredInUkPage).success.value

      val result = underTest.build(answers, "any-dprs-id", "any-operator-id")
      result.left.value.toChain.toList must contain theSameElementsAs Seq(
        BusinessNamePage,
        HasTradingNamePage,
        UkTaxIdentifiersPage,
        PrimaryContactNamePage,
        PrimaryContactEmailPage,
        CanPhonePrimaryContactPage,
        HasSecondaryContactPage,
        RegisteredInUkPage
      )
    }
  }
}
