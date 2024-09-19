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

import cats.data.{EitherNec, NonEmptyChain, NonEmptyList, StateT}
import cats.implicits._
import models.UkTaxIdentifiers._
import models.operator._
import models.operator.requests.{CreatePlatformOperatorRequest, Notification, UpdatePlatformOperatorRequest}
import models.operator.responses.{NotificationDetails, PlatformOperator}
import models.{Country, DueDiligence, InternationalAddress, UkAddress, UkTaxIdentifiers, UserAnswers}
import pages.add._
import pages.notification.{DueDiligencePage, NotificationTypePage, ReportingPeriodPage}
import play.api.libs.json.Writes
import queries.{NotificationDetailsQuery, Query, Settable}

import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class UserAnswersService @Inject() {

  def fromPlatformOperator(userId: String, platformOperator: PlatformOperator): Try[UserAnswers] = {

    val transformation = for {
      _ <- set(BusinessNamePage, platformOperator.operatorName)
      _ <- set(HasTradingNamePage, platformOperator.tradingName.isDefined)
      _ <- setOptional(TradingNamePage, platformOperator.tradingName)
      _ <- setTaxIdentifiers(platformOperator.tinDetails)
      _ <- setAddress(platformOperator.addressDetails)
      _ <- setPrimaryContact(platformOperator.primaryContactDetails)
      _ <- setSecondaryContact(platformOperator.secondaryContactDetails)
      _ <- set(NotificationDetailsQuery, platformOperator.notifications)
    } yield ()

    transformation.runS(UserAnswers(userId, Some(platformOperator.operatorId)))
  }

  private def set[A](settable: Settable[A], value: A)(implicit writes: Writes[A]): StateT[Try, UserAnswers, Unit] =
    StateT.modifyF[Try, UserAnswers](_.set(settable, value))

  private def setOptional[A](settable: Settable[A], optionalValue: Option[A])(implicit writes: Writes[A]): StateT[Try, UserAnswers, Unit] =
    optionalValue.map { value =>
      set(settable, value)
    }.getOrElse(StateT.pure(()))

  private def setTaxIdentifiers(tinDetails: Seq[TinDetails]): StateT[Try, UserAnswers, Unit] =
    NonEmptyList.fromList(tinDetails.toList).map { tins =>
      if (tins.exists(_.issuedBy == "GB")) setUkTaxIdentifierDetails(tins) else setInternationalTaxIdentifierDetails(tins)
    }.getOrElse(set(HasTaxIdentifierPage, false))

  private def setUkTaxIdentifierDetails(tinDetails: NonEmptyList[TinDetails])(implicit ev: Writes[String]): StateT[Try, UserAnswers, Unit] =
    for {
      _ <- set(HasTaxIdentifierPage, true)
      _ <- set(TaxResidentInUkPage, true)
      _ <- setUkTaxIdentifiers(tinDetails)
      _ <- setUkTaxIdentifierValue(tinDetails, TinType.Utr, UtrPage)
      _ <- setUkTaxIdentifierValue(tinDetails, TinType.Crn, CrnPage)
      _ <- setUkTaxIdentifierValue(tinDetails, TinType.Vrn, VrnPage)
      _ <- setUkTaxIdentifierValue(tinDetails, TinType.Empref, EmprefPage)
      _ <- setUkTaxIdentifierValue(tinDetails, TinType.Chrn, ChrnPage)

    } yield ()

  private def setUkTaxIdentifiers(tinDetails: NonEmptyList[TinDetails]): StateT[Try, UserAnswers, Unit] =
    tinDetails.toList.flatMap(tinToTaxIdentifier).toSet match {
      case ids if ids.nonEmpty => set(UkTaxIdentifiersPage, ids)
      case _                   => StateT.pure(())
    }

  private def tinToTaxIdentifier(tinDetails: TinDetails): Option[UkTaxIdentifiers] =
    tinDetails.tinType match {
      case TinType.Utr => Some(Utr)
      case TinType.Crn => Some(Crn)
      case TinType.Vrn => Some(Vrn)
      case TinType.Empref => Some(Empref)
      case TinType.Chrn => Some(Chrn)
      case _ => None
    }

  private def setUkTaxIdentifierValue(tinDetails: NonEmptyList[TinDetails], tinType: TinType, page: Settable[String])
                                     (implicit ev: Writes[String]): StateT[Try, UserAnswers, Unit] =
    tinDetails.find(_.tinType == tinType)
      .map(tin => set(page, tin.tin))
      .getOrElse(StateT.pure(()))

  private def setInternationalTaxIdentifierDetails(tinDetails: NonEmptyList[TinDetails]): StateT[Try, UserAnswers, Unit] =
    Country.internationalCountries.find(_.code == tinDetails.head.issuedBy).map { country =>
      for {
        _ <- set(HasTaxIdentifierPage, true)
        _ <- set(TaxResidentInUkPage, false)
        _ <- set(TaxResidencyCountryPage, country)
        _ <- set(InternationalTaxIdentifierPage, tinDetails.head.tin)
      } yield ()
    }.getOrElse(set(HasTaxIdentifierPage, false))

  private def setAddress(address: AddressDetails): StateT[Try, UserAnswers, Unit] =
    address.countryCode.map { countryCode =>
      if (Country.ukCountries.map(_.code).contains(countryCode)) setUkAddress(address) else setInternationalAddress(address)
    }.getOrElse(StateT.pure(()))

  private def setUkAddress(address: AddressDetails): StateT[Try, UserAnswers, Unit] =
    address match {
      case AddressDetails(line1, line2, Some(line3), line4, Some(postCode), Some(countryCode)) =>
        Country.ukCountries.find(_.code == countryCode).map { country =>
          for {
            _ <- set(RegisteredInUkPage, true)
            _ <- set(UkAddressPage, UkAddress(line1, line2, line3, line4, postCode, country))
          } yield ()
        }.getOrElse(StateT.pure(()))

      case _ =>
        StateT.pure(())
    }

  private def setInternationalAddress(address: AddressDetails): StateT[Try, UserAnswers, Unit] =
    address match {
      case AddressDetails(line1, line2, Some(line3), line4, Some(postCode), Some(countryCode)) =>
        Country.internationalCountries.find(_.code == countryCode).map { country =>
          for {
            _ <- set(RegisteredInUkPage, false)
            _ <- set(InternationalAddressPage, InternationalAddress(line1, line2, line3, line4, postCode, country))
          } yield ()
        }.getOrElse(StateT.pure(()))

      case _ =>
        StateT.pure(())
    }

  private def setPrimaryContact(contact: ContactDetails): StateT[Try, UserAnswers, Unit] =
    for {
      _ <- set(PrimaryContactNamePage, contact.contactName)
      _ <- set(PrimaryContactEmailPage, contact.emailAddress)
      _ <- set(CanPhonePrimaryContactPage, contact.phoneNumber.isDefined)
      _ <- setOptional(PrimaryContactPhoneNumberPage, contact.phoneNumber)
    } yield ()

  private def setSecondaryContact(optionalContact: Option[ContactDetails]): StateT[Try, UserAnswers, Unit] = {
    optionalContact.map { contact =>
        for {
          _ <- set(HasSecondaryContactPage, true)
          _ <- set(SecondaryContactNamePage, contact.contactName)
          _ <- set(SecondaryContactEmailPage, contact.emailAddress)
          _ <- set(CanPhoneSecondaryContactPage, contact.phoneNumber.isDefined)
          _ <- setOptional(SecondaryContactPhoneNumberPage, contact.phoneNumber)
        } yield ()
    }.getOrElse(set(HasSecondaryContactPage, false))
  }

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

  def toUpdatePlatformOperatorRequest(answers: UserAnswers, dprsId: String, operatorId: String): EitherNec[Query, UpdatePlatformOperatorRequest] =
    (
      answers.getEither(BusinessNamePage),
      getTradingName(answers),
      getTinDetails(answers),
      getPrimaryContact(answers),
      getSecondaryContact(answers),
      getAddressDetails(answers)
    ).parMapN { (operatorName, tradingName, tinDetails, primaryContact, secondaryContact, addressDetails) =>
      UpdatePlatformOperatorRequest(dprsId, operatorId, operatorName, tinDetails, None, tradingName, primaryContact, secondaryContact, addressDetails, None)
    }

  def addNotificationRequest(answers: UserAnswers, dprsId: String, operatorId: String): EitherNec[Query, UpdatePlatformOperatorRequest] =
    (
      toUpdatePlatformOperatorRequest(answers, dprsId, operatorId),
      getNotificationDetails(answers)
    ).parMapN { (baseRequest, notification) =>
      baseRequest.copy(notification = Some(notification))
    }

  private def getTradingName(answers: UserAnswers): EitherNec[Query, Option[String]] =
    answers.getEither(HasTradingNamePage).flatMap {
      case true => answers.getEither(TradingNamePage).map(Some(_))
      case false => Right(None)
    }

  private def getTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] = {
    answers.getEither(HasTaxIdentifierPage).flatMap {
      case true =>
        answers.getEither(TaxResidentInUkPage).flatMap {
          case true  => getUkTinDetails(answers)
          case false => getInternationalTinDetails(answers)
        }

      case false =>
        Right(Seq.empty)
    }
  }

  private def getUkTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(UkTaxIdentifiersPage).flatMap { identifiers =>
      identifiers.toSeq.parTraverse(getUkTin(_, answers))
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
    (
      answers.getEither(TaxResidencyCountryPage),
      answers.getEither(InternationalTaxIdentifierPage)
    ).parMapN { (country, identifier) =>
      Seq(TinDetails(identifier, TinType.Other, country.code))
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
          AddressDetails(address.line1, address.line2, Some(address.city), address.region, Some(address.postal), Some(address.country.code))
        }
    }

  private def getNotificationDetails(answers: UserAnswers): EitherNec[Query, Notification] =
    answers.getEither(NotificationTypePage).flatMap {
      case models.NotificationType.Rpo =>
        (
          answers.getEither(DueDiligencePage),
          answers.getEither(ReportingPeriodPage)
        ).parMapN { (dueDiligence, reportingPeriod) =>
          Notification(
            notificationType = NotificationType.Rpo,
            isDueDiligence = Some(dueDiligence.contains(DueDiligence.Extended)),
            isActiveSeller = Some(dueDiligence.contains(DueDiligence.ActiveSeller)),
            firstPeriod = reportingPeriod
          )
        }

      case models.NotificationType.Epo =>
        answers.getEither(ReportingPeriodPage).flatMap { reportingPeriod =>
          Right(Notification(NotificationType.Epo, None, None, reportingPeriod))
        }
    }
}

object UserAnswersService {

  final case class BuildCreatePlatformOperatorRequestFailure(errors: NonEmptyChain[Query]) extends Throwable {
    override def getMessage: String = s"unable to build Create Platform Operator request, path(s) missing: ${errors.toChain.toList.map(_.path).mkString(", ")}"
  }

  final case class BuildUpdatePlatformOperatorRequestFailure(errors: NonEmptyChain[Query]) extends Throwable {
    override def getMessage: String = s"unable to build Update Platform Operator request, path(s) missing: ${errors.toChain.toList.map(_.path).mkString(", ")}"
  }

  final case class BuildAddNotificationRequestFailure(errors: NonEmptyChain[Query]) extends Throwable {
    override def getMessage: String = s"unable to build Add Notification request, path(s) missing: ${errors.toChain.toList.map(_.path).mkString(", ")}"
  }
}
