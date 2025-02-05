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

package models.operator.requests

import cats.data.EitherNec
import cats.implicits._
import models.Country.UnitedKingdom
import models.RegisteredAddressCountry.{International, JerseyGuernseyIsleOfMan, Uk}
import models.UkTaxIdentifiers.{Chrn, Crn, Empref, Utr, Vrn}
import models.operator.{AddressDetails, ContactDetails, TinDetails, TinType}
import models.{UkTaxIdentifiers, UserAnswers}
import pages.add._
import play.api.libs.json.{Json, OFormat}
import queries.Query

final case class CreatePlatformOperatorRequest(subscriptionId: String,
                                               operatorName: String,
                                               tinDetails: Seq[TinDetails],
                                               businessName: Option[String],
                                               tradingName: Option[String],
                                               primaryContactDetails: ContactDetails,
                                               secondaryContactDetails: Option[ContactDetails],
                                               addressDetails: AddressDetails)

object CreatePlatformOperatorRequest {
  implicit lazy val format: OFormat[CreatePlatformOperatorRequest] = Json.format

  def build(answers: UserAnswers, dprsId: String): EitherNec[Query, CreatePlatformOperatorRequest] =
    (
      answers.getEither(BusinessNamePage),
      getTradingName(answers),
      getUkTinDetails(answers),
      getPrimaryContact(answers),
      getSecondaryContact(answers),
      getAddressDetails(answers)
    ).parMapN { (operatorName, tradingName, tinDetails, primaryContact, secondaryContact, addressDetails) =>
      CreatePlatformOperatorRequest(dprsId, operatorName, tinDetails, None, tradingName, primaryContact, secondaryContact, addressDetails)
    }

  private[requests] def getTradingName(answers: UserAnswers): EitherNec[Query, Option[String]] =
    answers.getEither(HasTradingNamePage).flatMap {
      case true => answers.getEither(TradingNamePage).map(Some(_))
      case false => Right(None)
    }

  private[requests] def getUkTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(UkTaxIdentifiersPage)
      .flatMap(identifiers => identifiers.toSeq.parTraverse(getUkTin(_, answers)))

  private[requests] def getUkTin(identifier: UkTaxIdentifiers, answers: UserAnswers): EitherNec[Query, TinDetails] = identifier match {
    case Utr => answers.getEither(UtrPage).map(tin => TinDetails(tin, TinType.Utr, UnitedKingdom.code))
    case Crn => answers.getEither(CrnPage).map(tin => TinDetails(tin, TinType.Crn, UnitedKingdom.code))
    case Vrn => answers.getEither(VrnPage).map(tin => TinDetails(tin, TinType.Vrn, UnitedKingdom.code))
    case Empref => answers.getEither(EmprefPage).map(tin => TinDetails(tin, TinType.Empref, UnitedKingdom.code))
    case Chrn => answers.getEither(ChrnPage).map(tin => TinDetails(tin, TinType.Chrn, UnitedKingdom.code))
  }

  private[requests] def getPrimaryContact(answers: UserAnswers): EitherNec[Query, ContactDetails] =
    (
      answers.getEither(PrimaryContactNamePage),
      answers.getEither(PrimaryContactEmailPage),
      getPrimaryContactPhone(answers)
    ).parMapN { (name, email, phone) =>
      ContactDetails(phone, name, email)
    }

  private[requests] def getPrimaryContactPhone(answers: UserAnswers): EitherNec[Query, Option[String]] =
    answers.getEither(CanPhonePrimaryContactPage).flatMap {
      case true => answers.getEither(PrimaryContactPhoneNumberPage).map(Some(_))
      case false => Right(None)
    }

  private[requests] def getSecondaryContact(answers: UserAnswers): EitherNec[Query, Option[ContactDetails]] =
    answers.getEither(HasSecondaryContactPage).flatMap {
      case true => (
        answers.getEither(SecondaryContactNamePage),
        answers.getEither(SecondaryContactEmailPage),
        getSecondaryContactPhone(answers)
      ).parMapN { (name, email, phone) =>
        Some(ContactDetails(phone, name, email))
      }
      case false => Right(None)
    }

  private[requests] def getSecondaryContactPhone(answers: UserAnswers): EitherNec[Query, Option[String]] =
    answers.getEither(CanPhoneSecondaryContactPage).flatMap {
      case true => answers.getEither(SecondaryContactPhoneNumberPage).map(Some(_))
      case false => Right(None)
    }

  private[requests] def getAddressDetails(answers: UserAnswers): EitherNec[Query, AddressDetails] =
    answers.getEither(RegisteredInUkPage).flatMap {
      case Uk => answers.getEither(UkAddressPage).map(AddressDetails(_))
      case JerseyGuernseyIsleOfMan => answers.getEither(JerseyGuernseyIoMAddressPage).map(AddressDetails(_))
      case International => answers.getEither(InternationalAddressPage).map(AddressDetails(_))
    }
}
