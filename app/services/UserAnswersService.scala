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

import cats.data.{EitherNec, NonEmptyChain}
import cats.implicits._
import models.{UkTaxIdentifiers, UserAnswers}
import models.UkTaxIdentifiers._
import models.requests.operator._
import models.requests.operator.requests.CreatePlatformOperatorRequest
import pages.add._
import queries.Query

import javax.inject.{Inject, Singleton}

@Singleton
class UserAnswersService @Inject() {

  def toCreatePlatformOperatorRequest(answers: UserAnswers, dprsId: String): EitherNec[Query, CreatePlatformOperatorRequest] =
    (
      answers.getEither(BusinessNamePage),
      getTradingName(answers),
      getTinDetails(answers),
      getPrimaryContact(answers),
      getSecondaryContact(answers),
      getAddressDetails(answers)
    ).parMapN { (operatorName, tradingName, tinDetails, primaryContact, secondaryContact, addressDetails) =>
      CreatePlatformOperatorRequest(dprsId, operatorName, tinDetails, None, tradingName, primaryContact, secondaryContact, addressDetails)
    }

  private def getTradingName(answers: UserAnswers): EitherNec[Query, Option[String]] =
    answers.getEither(HasTradingNamePage).flatMap {
      case true => answers.getEither(TradingNamePage).map(Some(_))
      case false => Right(None)
    }

  private def getTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(TaxResidentInUkPage).flatMap {
      case true =>
        answers.getEither(HasUkTaxIdentifierPage).flatMap {
          case true  => getUkTinDetails(answers)
          case false => Right(Seq.empty)
        }

      case false =>
        answers.getEither(HasInternationalTaxIdentifierPage).flatMap {
          case true  => getInternationalTinDetails(answers)
          case false => Right(Seq.empty)
        }
    }

  private def getUkTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(UkTaxIdentifiersPage).flatMap { identifiers =>
      (
        if (identifiers.contains(Utr))    getUkTin(Utr, answers).map(Some(_)) else Right(None),
        if (identifiers.contains(Crn))    getUkTin(Crn, answers).map(Some(_)) else Right(None),
        if (identifiers.contains(Vrn))    getUkTin(Vrn, answers).map(Some(_)) else Right(None),
        if (identifiers.contains(Empref)) getUkTin(Empref, answers).map(Some(_)) else Right(None),
        if (identifiers.contains(Chrn))   getUkTin(Chrn, answers).map(Some(_)) else Right(None)
      ).parMapN { (utr, crn, vrn, empref, chrn) =>
        Seq(utr, crn, vrn, empref, chrn).flatten
      }
    }

  private def getUkTin(identifier: UkTaxIdentifiers, answers: UserAnswers): EitherNec[Query, TinDetails] = {
    identifier match {
      case Utr    => answers.getEither(UtrPage).map(tin => TinDetails(tin, TinType.Utr, "GB"))
      case Crn    => answers.getEither(CrnPage).map(tin => TinDetails(tin, TinType.Crn, "GB"))
      case Vrn    => answers.getEither(VrnPage).map(tin => TinDetails(tin, TinType.Vrn, "GB"))
      case Empref => answers.getEither(EmprefPage).map(tin => TinDetails(tin, TinType.Empref, "GB"))
      case Chrn   => answers.getEither(ChrnPage).map(tin => TinDetails(tin, TinType.Chrn, "GB"))
    }
  }

  private def getInternationalTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(HasInternationalTaxIdentifierPage).flatMap {
      case true =>
        (
          answers.getEither(TaxResidencyCountryPage),
          answers.getEither(InternationalTaxIdentifierPage)
        ).parMapN { (country, identifier) =>
          Seq(TinDetails(identifier, TinType.Other, country.code))
        }

      case false =>
        Right(Seq.empty)
    }

  private def getPrimaryContact(answers: UserAnswers): EitherNec[Query, ContactDetails] =
    (
      answers.getEither(PrimaryContactNamePage),
      answers.getEither(PrimaryContactEmailPage),
      getPrimaryContactPhone(answers)
    ).parMapN { (name, email, phone) =>
      ContactDetails(phone, name, email)
    }

  private def getPrimaryContactPhone(answers: UserAnswers): EitherNec[Query, Option[String]] =
    answers.getEither(CanPhonePrimaryContactPage).flatMap {
      case true => answers.getEither(PrimaryContactPhoneNumberPage).map(Some(_))
      case false => Right(None)
    }

  private def getSecondaryContact(answers: UserAnswers): EitherNec[Query, Option[ContactDetails]] =
    answers.getEither(HasSecondaryContactPage).flatMap {
      case true =>
        (
          answers.getEither(SecondaryContactNamePage),
          answers.getEither(SecondaryContactEmailPage),
          getSecondaryContactPhone(answers)
        ).parMapN { (name, email, phone) =>
          Some(ContactDetails(phone, name, email))
        }
        
      case false =>
        Right(None)
    }

  private def getSecondaryContactPhone(answers: UserAnswers): EitherNec[Query, Option[String]] =
    answers.getEither(CanPhoneSecondaryContactPage).flatMap {
      case true => answers.getEither(SecondaryContactPhoneNumberPage).map(Some(_))
      case false => Right(None)
    }

  private def getAddressDetails(answers: UserAnswers): EitherNec[Query, AddressDetails] =
    answers.getEither(RegisteredInUkPage).flatMap {
      case true =>
        answers.getEither(UkAddressPage).map { address =>
          AddressDetails(address.line1, address.line2, Some(address.town), address.county, Some(address.postCode), Some(address.country.code))
        }

      case false =>
        answers.getEither(InternationalAddressPage).map { address =>
          AddressDetails(address.line1, address.line2, Some(address.city), address.region, address.postal, Some(address.country.code))
        }
    }
}

object UserAnswersService {

  final case class BuildCreatePlatformOperatorRequestFailure(errors: NonEmptyChain[Query]) extends Throwable {
    override def getMessage: String = s"unable to build Create Platform Operator request, path(s) missing: ${errors.toChain.toList.map(_.path).mkString(", ")}"
  }
}
