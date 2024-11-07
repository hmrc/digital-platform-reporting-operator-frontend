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

package models.email.requests

import cats.data.EitherNec
import cats.implicits._
import models.operator.NotificationType.Rpo
import models.UserAnswers
import models.email.requests.SendEmailRequest.getContactName
import models.subscription.{IndividualContact, OrganisationContact, SubscriptionInfo}
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}
import pages.notification.ReportingPeriodPage
import play.api.libs.json.{Json, OFormat}
import queries.Query
import models.operator.requests.UpdatePlatformOperatorRequest

sealed trait SendEmailRequest {
  def to: List[String]

  def templateId: String

  def parameters: Map[String, String]
}

object SendEmailRequest {
  implicit val format: OFormat[SendEmailRequest] = Json.format[SendEmailRequest]

  def getContactName(subscriptionInfo: SubscriptionInfo): String = subscriptionInfo.primaryContact match {
    case ic: IndividualContact => ic.individual.firstName + " " + ic.individual.lastName
    case oc: OrganisationContact => oc.organisation.name
  }

}

final case class AddedPlatformOperatorRequest(to: List[String],
                                              templateId: String,
                                              parameters: Map[String, String]) extends SendEmailRequest

object AddedPlatformOperatorRequest {
  implicit val format: OFormat[AddedPlatformOperatorRequest] = Json.format[AddedPlatformOperatorRequest]
  private val PlatformOperatorAddedTemplateId: String = "dprs_added_platform_operator"

  def apply(email: String,
            name: String,
            businessName: String,
            platformOperatorId: String): AddedPlatformOperatorRequest = AddedPlatformOperatorRequest(
    to = List(email),
    templateId = PlatformOperatorAddedTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "poBusinessName" -> businessName,
      "poId" -> platformOperatorId)
  )

  def build(userAnswers: UserAnswers, subscriptionInfo: SubscriptionInfo): EitherNec[Query, AddedPlatformOperatorRequest] = {
    val contactName: String = getContactName(subscriptionInfo)
    (
      Right(subscriptionInfo.primaryContact.email),
      userAnswers.getEither(BusinessNamePage)
    ).parMapN(AddedPlatformOperatorRequest(_, contactName, _ , userAnswers.operatorId.get))
  }
}

final case class AddedAsPlatformOperatorRequest(to: List[String],
                                                templateId: String,
                                                parameters: Map[String, String]) extends SendEmailRequest

object AddedAsPlatformOperatorRequest {
  implicit val format: OFormat[AddedAsPlatformOperatorRequest] = Json.format[AddedAsPlatformOperatorRequest]
  private val PlatformOperatorAddedTemplateId: String = "dprs_added_as_platform_operator"

  def apply(email: String,
            platformOperatorContactName: String,
            platformOperatorId: String,
            platformOperatorBusinessName: String): AddedAsPlatformOperatorRequest = AddedAsPlatformOperatorRequest(
    to = List(email),
    templateId = PlatformOperatorAddedTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> platformOperatorContactName,
      "poId"                 -> platformOperatorId,
      "poBusinessName"       -> platformOperatorBusinessName
    )
  )

  def build(userAnswers: UserAnswers): EitherNec[Query, AddedPlatformOperatorRequest] = (
    userAnswers.getEither(PrimaryContactEmailPage),
    userAnswers.getEither(PrimaryContactNamePage),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(AddedPlatformOperatorRequest(_, _, _, userAnswers.operatorId.get))

}

final case class RemovedPlatformOperatorRequest(to: List[String],
                                              templateId: String,
                                              parameters: Map[String, String]) extends SendEmailRequest

object RemovedPlatformOperatorRequest {
  implicit val format: OFormat[RemovedPlatformOperatorRequest] = Json.format[RemovedPlatformOperatorRequest]
  private val PlatformOperatorRemovedTemplateId: String = "dprs_removed_platform_operator"

  def apply(email: String,
            name: String,
            businessName: String,
            platformOperatorId: String): RemovedPlatformOperatorRequest = RemovedPlatformOperatorRequest(
    to = List(email),
    templateId = PlatformOperatorRemovedTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "poBusinessName" -> businessName,
      "poId" -> platformOperatorId)
  )

  def build(userAnswers: UserAnswers, subscriptionInfo: SubscriptionInfo): EitherNec[Query, RemovedPlatformOperatorRequest] = {
    val contactName: String = getContactName(subscriptionInfo)
    (
      Right(subscriptionInfo.primaryContact.email),
      userAnswers.getEither(BusinessNamePage)
    ).parMapN(RemovedPlatformOperatorRequest(_, contactName, _ , userAnswers.operatorId.get))
  }
}

final case class RemovedAsPlatformOperatorRequest(to: List[String],
                                                templateId: String,
                                                parameters: Map[String, String]) extends SendEmailRequest

object RemovedAsPlatformOperatorRequest {
  implicit val format: OFormat[RemovedAsPlatformOperatorRequest] = Json.format[RemovedAsPlatformOperatorRequest]
  private val PlatformOperatorRemovedTemplateId: String = "dprs_removed_as_platform_operator"

  def apply(email: String,
            name: String,
            businessName: String,
            platformOperatorId: String): RemovedAsPlatformOperatorRequest = RemovedAsPlatformOperatorRequest(
    to = List(email),
    templateId = PlatformOperatorRemovedTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> name,
      "poBusinessName" -> businessName,
      "poId" -> platformOperatorId)
  )

  def build(userAnswers: UserAnswers): EitherNec[Query, RemovedAsPlatformOperatorRequest] = (
    userAnswers.getEither(PrimaryContactEmailPage),
    userAnswers.getEither(PrimaryContactNamePage),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(RemovedAsPlatformOperatorRequest(_, _, _, userAnswers.operatorId.get))
}

final case class UpdatedPlatformOperatorRequest(to: List[String],
                                                templateId: String,
                                                parameters: Map[String, String]) extends SendEmailRequest

object UpdatedPlatformOperatorRequest {
  implicit val format: OFormat[UpdatedPlatformOperatorRequest] = Json.format[UpdatedPlatformOperatorRequest]
  private val PlatformOperatorUpdatedTemplateId: String = "dprs_updated_platform_operator"

  def apply(email: String,
            name: String,
            businessName: String,
            platformOperatorId: String): UpdatedPlatformOperatorRequest = UpdatedPlatformOperatorRequest(
    to = List(email),
    templateId = PlatformOperatorUpdatedTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "poBusinessName" -> businessName,
      "poId" -> platformOperatorId)
  )

  def build(userAnswers: UserAnswers, subscriptionInfo: SubscriptionInfo): EitherNec[Query, UpdatedPlatformOperatorRequest] = {
    val contactName: String = getContactName(subscriptionInfo)
    (
      Right(subscriptionInfo.primaryContact.email),
      userAnswers.getEither(BusinessNamePage)
    ).parMapN(UpdatedPlatformOperatorRequest(_, contactName, _ , userAnswers.operatorId.get))
  }
}

final case class UpdatedAsPlatformOperatorRequest(to: List[String],
                                                  templateId: String,
                                                  parameters: Map[String, String]) extends SendEmailRequest

object UpdatedAsPlatformOperatorRequest {
  implicit val format: OFormat[UpdatedAsPlatformOperatorRequest] = Json.format[UpdatedAsPlatformOperatorRequest]
  private val PlatformOperatorUpdatedTemplateId: String = "dprs_updated_as_platform_operator"

  def apply(email: String,
            name: String,
            businessName: String,
            platformOperatorId: String): UpdatedAsPlatformOperatorRequest = UpdatedAsPlatformOperatorRequest(
    to = List(email),
    templateId = PlatformOperatorUpdatedTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> name,
      "poBusinessName" -> businessName,
      "poId" -> platformOperatorId)
  )

  def build(userAnswers: UserAnswers): EitherNec[Query, UpdatedAsPlatformOperatorRequest] = (
    userAnswers.getEither(PrimaryContactEmailPage),
    userAnswers.getEither(PrimaryContactNamePage),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(UpdatedAsPlatformOperatorRequest(_, _, _, userAnswers.operatorId.get))
}

final case class AddedReportingNotificationRequest(to: List[String],
                                                templateId: String,
                                                parameters: Map[String, String]) extends SendEmailRequest

object AddedReportingNotificationRequest {
  implicit val format: OFormat[AddedReportingNotificationRequest] = Json.format[AddedReportingNotificationRequest]
  private val PlatformOperatorAddedNotificationTemplateId: String = "dprs_added_reporting_notification"

  def apply(email: String,
            name: String,
            businessName: String): AddedReportingNotificationRequest = AddedReportingNotificationRequest(
    to = List(email),
    templateId = PlatformOperatorAddedNotificationTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "poBusinessName" -> businessName)
  )

  def build(userAnswers: UserAnswers, subscriptionInfo: SubscriptionInfo): EitherNec[Query, AddedReportingNotificationRequest] = {
    val contactName: String = getContactName(subscriptionInfo)
    (
      Right(subscriptionInfo.primaryContact.email),
      userAnswers.getEither(BusinessNamePage)
    ).parMapN(AddedReportingNotificationRequest(_, contactName, _))
  }

}

final case class AddedAsReportingNotificationRequest(to: List[String],
                                                  templateId: String,
                                                  parameters: Map[String, String]) extends SendEmailRequest

object AddedAsReportingNotificationRequest {

  implicit val format: OFormat[AddedAsReportingNotificationRequest] = Json.format[AddedAsReportingNotificationRequest]
  private val PlatformOperatorAddedNotificationTemplateId: String = "dprs_added_reporting_notification_for_you"

  def apply(email: String,
            name: String,
            isReportingPO: Boolean,
            reportablePeriodYear: Int,
            poBusinessName: String,
            isExtendedDueDiligence: Boolean,
            isActiveSellerDueDiligence: Boolean
           ): AddedAsReportingNotificationRequest = {

    AddedAsReportingNotificationRequest(
      to = List(email),
      templateId = PlatformOperatorAddedNotificationTemplateId,
      parameters = Map(
        "poPrimaryContactName" -> name,
        "isReportingPO" -> isReportingPO.toString,
        "reportablePeriodYear" -> reportablePeriodYear.toString,
        "poBusinessName" -> poBusinessName,
        "isExtendedDueDiligence" -> isExtendedDueDiligence.toString,
        "isActiveSellerDueDiligence" -> isActiveSellerDueDiligence.toString
      )
    )
  }

  def build(userAnswers: UserAnswers, addNotificationRequest: UpdatePlatformOperatorRequest): EitherNec[Query, AddedAsReportingNotificationRequest] = {
    val isRPO = addNotificationRequest.notification.exists(notification =>
      notification.notificationType == Rpo)
    val isExtendedDueDiligence = addNotificationRequest.notification.flatMap(nt => nt.isDueDiligence).getOrElse(false)
    val isActiveSellerDueDiligence = addNotificationRequest.notification.flatMap(nt => nt.isActiveSeller).getOrElse(false)
    (
      userAnswers.getEither(PrimaryContactEmailPage),
      userAnswers.getEither(PrimaryContactNamePage),
      userAnswers.getEither(ReportingPeriodPage),
      userAnswers.getEither(BusinessNamePage)
    ).parMapN(AddedAsReportingNotificationRequest(_, _, isRPO, _, _, isExtendedDueDiligence, isActiveSellerDueDiligence))
  }

}

