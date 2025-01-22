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
import models.Country.UnitedKingdom
import models.UkTaxIdentifiers._
import models.RegisteredAddressCountry.{International, JerseyGuernseyIsleOfMan, Uk}
import models.operator._
import models.operator.requests.{CreatePlatformOperatorRequest, Notification, UpdatePlatformOperatorRequest}
import models.operator.responses.PlatformOperator
import models.{CountriesList, DueDiligence, InternationalAddress, JerseyGuernseyIoMAddress, UkAddress, UkTaxIdentifiers, UserAnswers}
import pages.add._
import pages.{update => updatePage}
import pages.notification.{DueDiligencePage, NotificationTypePage, ReportingPeriodPage}
import play.api.libs.json.Writes
import queries.{NotificationDetailsQuery, Query, Settable}

import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class UserAnswersService @Inject()(countriesList: CountriesList) {
  val update = "update"
  val add = "add"

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
    NonEmptyList.fromList(tinDetails.toList).map { tins =>setUkTaxIdentifierDetails(tins)
    }.getOrElse(StateT.pure(()))

  private def setUkTaxIdentifierDetails(tinDetails: NonEmptyList[TinDetails])(implicit ev: Writes[String]): StateT[Try, UserAnswers, Unit] =
    for {
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

  private def setAddress(address: AddressDetails): StateT[Try, UserAnswers, Unit] = {
    address.countryCode.map { countryCode =>
      if (UnitedKingdom.code.contains(countryCode)) {setUkAddress(address)}
      else if (countriesList.crownDependantCountries.map(_.code).contains(countryCode)) {setJerseyGuernseyIoMAddress(address)}
      else {setInternationalAddress(address)}
    }.getOrElse(StateT.pure(()))
  }

  private def setUkAddress(address: AddressDetails): StateT[Try, UserAnswers, Unit] =
    address match {
      case AddressDetails(line1, line2, Some(line3), line4, Some(postCode), Some(countryCode)) =>
        for {
          _ <- set(RegisteredInUkPage, Uk)
          _ <- set(UkAddressPage, UkAddress(line1, line2, line3, line4, postCode, UnitedKingdom))
        } yield ()

      case _ =>
        StateT.pure(())
    }

  private def setJerseyGuernseyIoMAddress(address: AddressDetails): StateT[Try, UserAnswers, Unit] =
    address match {
      case AddressDetails(line1, line2, Some(line3), line4, Some(postCode), Some(countryCode)) =>
        countriesList.crownDependantCountries.find(_.code == countryCode).map { country =>
          for {
            _ <- set(RegisteredInUkPage, JerseyGuernseyIsleOfMan)
            _ <- set(JerseyGuernseyIoMAddressPage, JerseyGuernseyIoMAddress(line1, line2, line3, line4, postCode, country))
          } yield ()
        }.getOrElse(StateT.pure(()))

      case _ =>
        StateT.pure(())
    }

  private def setInternationalAddress(address: AddressDetails): StateT[Try, UserAnswers, Unit] =
    address match {
      case AddressDetails(line1, line2, Some(line3), line4, Some(postCode), Some(countryCode)) =>
        countriesList.internationalCountries.find(_.code == countryCode).map { country =>
          for {
            _ <- set(RegisteredInUkPage, International)
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
      getTradingName(answers, add),
      getUkTinDetails(answers, add),
      getPrimaryContact(answers, add),
      getSecondaryContact(answers, add),
      getAddressDetails(answers)
    ).parMapN { (operatorName, tradingName, tinDetails, primaryContact, secondaryContact, addressDetails) =>
      CreatePlatformOperatorRequest(dprsId, operatorName, tinDetails, None, tradingName, primaryContact, secondaryContact, addressDetails)
    }

  def toUpdatePlatformOperatorRequest(answers: UserAnswers, dprsId: String, operatorId: String): EitherNec[Query, UpdatePlatformOperatorRequest] =
    (
      answers.getEither(updatePage.BusinessNamePage),
      getTradingName(answers, update),
      getUkTinDetails(answers, update),
      getPrimaryContact(answers, update),
      getSecondaryContact(answers, update),
      getUpdatePageAddressDetails(answers)
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

  private def getTradingName(answers: UserAnswers, pageType: String): EitherNec[Query, Option[String]] = {
    pageType match {
      case pageType if pageType.contains(add) => answers.getEither(HasTradingNamePage).flatMap {
        case true => answers.getEither(TradingNamePage).map(Some(_))
        case false => Right(None)
      }

      case pageType if pageType.contains(update) => answers.getEither(updatePage.HasTradingNamePage).flatMap {
        case true => answers.getEither(updatePage.TradingNamePage).map(Some(_))
        case false => Right(None)
      }
    }
  }

  private def getUkTinDetails(answers: UserAnswers, pageType: String): EitherNec[Query, Seq[TinDetails]] = {
    pageType match {
      case pageType if pageType.contains(add) => answers.getEither(UkTaxIdentifiersPage).flatMap { identifiers =>
        identifiers.toSeq.parTraverse(getUkTin(_, answers))}

      case pageType if pageType.contains(update) => answers.getEither(updatePage.UkTaxIdentifiersPage).flatMap { identifiers =>
        identifiers.toSeq.parTraverse(getUpdatePageUkTin(_, answers))}
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

  private def getUpdatePageUkTin(identifier: UkTaxIdentifiers, answers: UserAnswers): EitherNec[Query, TinDetails] = {
    identifier match {
      case Utr    => answers.getEither(updatePage.UtrPage).map(tin => TinDetails(tin, TinType.Utr, "GB"))
      case Crn    => answers.getEither(updatePage.CrnPage).map(tin => TinDetails(tin, TinType.Crn, "GB"))
      case Vrn    => answers.getEither(updatePage.VrnPage).map(tin => TinDetails(tin, TinType.Vrn, "GB"))
      case Empref => answers.getEither(updatePage.EmprefPage).map(tin => TinDetails(tin, TinType.Empref, "GB"))
      case Chrn   => answers.getEither(updatePage.ChrnPage).map(tin => TinDetails(tin, TinType.Chrn, "GB"))
    }
  }

  private def getPrimaryContact(answers: UserAnswers, pageType: String): EitherNec[Query, ContactDetails] = {
    pageType match {
      case pageType if pageType.contains(add) => (
        answers.getEither(PrimaryContactNamePage),
        answers.getEither(PrimaryContactEmailPage),
        getPrimaryContactPhone(answers, pageType)).parMapN { (name, email, phone) => ContactDetails(phone, name, email)}

      case pageType if pageType.contains(update) => (
        answers.getEither(updatePage.PrimaryContactNamePage),
        answers.getEither(updatePage.PrimaryContactEmailPage),
        getPrimaryContactPhone(answers, pageType)).parMapN { (name, email, phone) => ContactDetails(phone, name, email)}
    }
  }

  private def getPrimaryContactPhone(answers: UserAnswers, pageType: String): EitherNec[Query, Option[String]] = {
    pageType match {
      case pageType if pageType.contains(add) => answers.getEither(CanPhonePrimaryContactPage).flatMap {
        case true => answers.getEither(PrimaryContactPhoneNumberPage).map(Some(_))
        case false => Right(None)}

      case pageType if pageType.contains(update) => answers.getEither(updatePage.CanPhonePrimaryContactPage).flatMap {
        case true => answers.getEither(updatePage.PrimaryContactPhoneNumberPage).map(Some(_))
        case false => Right(None)}
    }
  }

  private def getSecondaryContact(answers: UserAnswers, pageType: String): EitherNec[Query, Option[ContactDetails]] = {
    pageType match {
      case pageType if pageType.contains(add) => answers.getEither(HasSecondaryContactPage).flatMap {
        case true => (
          answers.getEither(SecondaryContactNamePage),
          answers.getEither(SecondaryContactEmailPage),
          getSecondaryContactPhone(answers, pageType))
          .parMapN { (name, email, phone) => Some(ContactDetails(phone, name, email))}
        case false => Right(None)
      }

      case pageType if pageType.contains(update) => answers.getEither(updatePage.HasSecondaryContactPage).flatMap {
        case true => (
          answers.getEither(updatePage.SecondaryContactNamePage),
          answers.getEither(updatePage.SecondaryContactEmailPage),
          getSecondaryContactPhone(answers, pageType))
          .parMapN { (name, email, phone) => Some(ContactDetails(phone, name, email))}
        case false => Right(None)
      }
    }
  }

  private def getSecondaryContactPhone(answers: UserAnswers, pageType: String): EitherNec[Query, Option[String]] = {
    pageType match {
      case pageType if pageType.contains(add) => answers.getEither(CanPhoneSecondaryContactPage).flatMap {
        case true => answers.getEither(SecondaryContactPhoneNumberPage).map(Some(_))
        case false => Right(None)
      }

      case pageType if pageType.contains(update) => answers.getEither(updatePage.CanPhoneSecondaryContactPage).flatMap {
        case true => answers.getEither(updatePage.SecondaryContactPhoneNumberPage).map(Some(_))
        case false => Right(None)
      }
    }
  }

  private def getAddressDetails(answers: UserAnswers): EitherNec[Query, AddressDetails] =
    answers.getEither(RegisteredInUkPage).flatMap {
      case Uk =>
        answers.getEither(UkAddressPage).map { address =>
          AddressDetails(address.line1, address.line2, Some(address.town), address.county, Some(address.postCode), Some(address.country.code))
        }

      case JerseyGuernseyIsleOfMan =>
        answers.getEither(JerseyGuernseyIoMAddressPage).map { address =>
          AddressDetails(address.line1, address.line2, Some(address.town), address.county, Some(address.postCode), Some(address.country.code))
      }

      case International =>
        answers.getEither(InternationalAddressPage).map { address =>
          AddressDetails(address.line1, address.line2, Some(address.city), address.region, Some(address.postal), Some(address.country.code))
        }
    }

  private def getUpdatePageAddressDetails(answers: UserAnswers): EitherNec[Query, AddressDetails] =
    answers.getEither(updatePage.RegisteredInUkPage).flatMap {
      case Uk =>
        answers.getEither(updatePage.UkAddressPage).map { address =>
          AddressDetails(address.line1, address.line2, Some(address.town), address.county, Some(address.postCode), Some(address.country.code))
        }

      case JerseyGuernseyIsleOfMan =>
        answers.getEither(updatePage.JerseyGuernseyIoMAddressPage).map { address =>
          AddressDetails(address.line1, address.line2, Some(address.town), address.county, Some(address.postCode), Some(address.country.code))
        }

      case International =>
        answers.getEither(updatePage.InternationalAddressPage).map { address =>
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
