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
import models.UserAnswers
import models.operator.NotificationType.Rpo
import models.operator.requests.UpdatePlatformOperatorRequest
import models.subscription.SubscriptionInfo
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}
import pages.notification.ReportingPeriodPage
import play.api.libs.json.{Json, OFormat}
import queries.Query

sealed trait SendEmailRequest {
  def to: List[String]

  def templateId: String

  def parameters: Map[String, String]
}

object SendEmailRequest {
  implicit val format: OFormat[SendEmailRequest] = Json.format[SendEmailRequest]
}

final case class AddedPlatformOperatorRequest(to: List[String],
                                              templateId: String,
                                              parameters: Map[String, String]) extends SendEmailRequest

object AddedPlatformOperatorRequest {
  val AddedPlatformOperatorTemplateId: String = "dprs_added_platform_operator"
  implicit val format: OFormat[AddedPlatformOperatorRequest] = Json.format[AddedPlatformOperatorRequest]

  def apply(email: String,
            name: String,
            businessName: String,
            platformOperatorId: String): AddedPlatformOperatorRequest = AddedPlatformOperatorRequest(
    to = List(email),
    templateId = AddedPlatformOperatorTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "poBusinessName" -> businessName,
      "poId" -> platformOperatorId)
  )

  def build(userAnswers: UserAnswers, subscriptionInfo: SubscriptionInfo): EitherNec[Query, AddedPlatformOperatorRequest] = (
    Right(subscriptionInfo.primaryContact.email),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(AddedPlatformOperatorRequest(_, subscriptionInfo.primaryContactName, _, userAnswers.operatorId.get))
}

final case class AddedAsPlatformOperatorRequest(to: List[String],
                                                templateId: String,
                                                parameters: Map[String, String]) extends SendEmailRequest

object AddedAsPlatformOperatorRequest {
  val AddedAsPlatformOperatorTemplateId: String = "dprs_added_as_platform_operator"
  implicit val format: OFormat[AddedAsPlatformOperatorRequest] = Json.format[AddedAsPlatformOperatorRequest]

  def apply(email: String,
            platformOperatorContactName: String,
            platformOperatorId: String,
            platformOperatorBusinessName: String): AddedAsPlatformOperatorRequest = AddedAsPlatformOperatorRequest(
    to = List(email),
    templateId = AddedAsPlatformOperatorTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> platformOperatorContactName,
      "poId" -> platformOperatorId,
      "poBusinessName" -> platformOperatorBusinessName
    )
  )

  def build(userAnswers: UserAnswers): EitherNec[Query, AddedAsPlatformOperatorRequest] = (
    userAnswers.getEither(PrimaryContactEmailPage),
    userAnswers.getEither(PrimaryContactNamePage),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(AddedAsPlatformOperatorRequest(_, _, userAnswers.operatorId.get, _))
}

final case class RemovedPlatformOperatorRequest(to: List[String],
                                                templateId: String,
                                                parameters: Map[String, String]) extends SendEmailRequest

object RemovedPlatformOperatorRequest {
  val RemovedPlatformOperatorTemplateId: String = "dprs_removed_platform_operator"
  implicit val format: OFormat[RemovedPlatformOperatorRequest] = Json.format[RemovedPlatformOperatorRequest]

  def apply(email: String,
            name: String,
            businessName: String,
            platformOperatorId: String): RemovedPlatformOperatorRequest = RemovedPlatformOperatorRequest(
    to = List(email),
    templateId = RemovedPlatformOperatorTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "poBusinessName" -> businessName,
      "poId" -> platformOperatorId)
  )

  def build(userAnswers: UserAnswers, subscriptionInfo: SubscriptionInfo): EitherNec[Query, RemovedPlatformOperatorRequest] = (
    Right(subscriptionInfo.primaryContact.email),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(RemovedPlatformOperatorRequest(_, subscriptionInfo.primaryContactName, _, userAnswers.operatorId.get))
}

final case class RemovedAsPlatformOperatorRequest(to: List[String],
                                                  templateId: String,
                                                  parameters: Map[String, String]) extends SendEmailRequest

object RemovedAsPlatformOperatorRequest {
  val RemovedAsPlatformOperatorTemplateId: String = "dprs_removed_as_platform_operator"
  implicit val format: OFormat[RemovedAsPlatformOperatorRequest] = Json.format[RemovedAsPlatformOperatorRequest]

  def apply(email: String,
            name: String,
            businessName: String,
            platformOperatorId: String): RemovedAsPlatformOperatorRequest = RemovedAsPlatformOperatorRequest(
    to = List(email),
    templateId = RemovedAsPlatformOperatorTemplateId,
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
  val UpdatedPlatformOperatorTemplateId: String = "dprs_updated_platform_operator"
  implicit val format: OFormat[UpdatedPlatformOperatorRequest] = Json.format[UpdatedPlatformOperatorRequest]

  def apply(email: String,
            name: String,
            businessName: String): UpdatedPlatformOperatorRequest = UpdatedPlatformOperatorRequest(
    to = List(email),
    templateId = UpdatedPlatformOperatorTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "poBusinessName" -> businessName)
  )

  def build(userAnswers: UserAnswers, subscriptionInfo: SubscriptionInfo): EitherNec[Query, UpdatedPlatformOperatorRequest] = (
    Right(subscriptionInfo.primaryContact.email),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(UpdatedPlatformOperatorRequest(_, subscriptionInfo.primaryContactName, _))
}

final case class UpdatedAsPlatformOperatorRequest(to: List[String],
                                                  templateId: String,
                                                  parameters: Map[String, String]) extends SendEmailRequest

object UpdatedAsPlatformOperatorRequest {
  val UpdatedAsPlatformOperatorTemplateId: String = "dprs_updated_as_platform_operator"
  implicit val format: OFormat[UpdatedAsPlatformOperatorRequest] = Json.format[UpdatedAsPlatformOperatorRequest]

  def apply(email: String,
            name: String,
            businessName: String): UpdatedAsPlatformOperatorRequest = UpdatedAsPlatformOperatorRequest(
    to = List(email),
    templateId = UpdatedAsPlatformOperatorTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> name,
      "poBusinessName" -> businessName)
  )

  def build(userAnswers: UserAnswers): EitherNec[Query, UpdatedAsPlatformOperatorRequest] = (
    userAnswers.getEither(PrimaryContactEmailPage),
    userAnswers.getEither(PrimaryContactNamePage),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(UpdatedAsPlatformOperatorRequest(_, _, _))
}

final case class AddedReportingNotificationRequest(to: List[String],
                                                   templateId: String,
                                                   parameters: Map[String, String]) extends SendEmailRequest

object AddedReportingNotificationRequest {
  val AddedReportingNotificationTemplateId: String = "dprs_added_reporting_notification"
  implicit val format: OFormat[AddedReportingNotificationRequest] = Json.format[AddedReportingNotificationRequest]

  def apply(email: String,
            name: String,
            businessName: String): AddedReportingNotificationRequest = AddedReportingNotificationRequest(
    to = List(email),
    templateId = AddedReportingNotificationTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "poBusinessName" -> businessName)
  )

  def build(userAnswers: UserAnswers, subscriptionInfo: SubscriptionInfo): EitherNec[Query, AddedReportingNotificationRequest] = (
    Right(subscriptionInfo.primaryContact.email),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(AddedReportingNotificationRequest(_, subscriptionInfo.primaryContactName, _))
}

final case class AddedAsReportingNotificationRequest(to: List[String],
                                                     templateId: String,
                                                     parameters: Map[String, String]) extends SendEmailRequest

object AddedAsReportingNotificationRequest {
  val AddedAsReportingNotificationTemplateId: String = "dprs_added_reporting_notification_for_you"
  implicit val format: OFormat[AddedAsReportingNotificationRequest] = Json.format[AddedAsReportingNotificationRequest]

  def apply(email: String,
            name: String,
            isReportingPO: Boolean,
            reportablePeriodYear: Int,
            poBusinessName: String,
            isExtendedDueDiligence: Boolean,
            isActiveSellerDueDiligence: Boolean): AddedAsReportingNotificationRequest = {

    AddedAsReportingNotificationRequest(
      to = List(email),
      templateId = AddedAsReportingNotificationTemplateId,
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
    val isRPO = addNotificationRequest.notification.exists(_.notificationType == Rpo)
    val isExtendedDueDiligence = addNotificationRequest.notification.flatMap(_.isDueDiligence).getOrElse(false)
    val isActiveSellerDueDiligence = addNotificationRequest.notification.flatMap(_.isActiveSeller).getOrElse(false)
    (
      userAnswers.getEither(PrimaryContactEmailPage),
      userAnswers.getEither(PrimaryContactNamePage),
      userAnswers.getEither(ReportingPeriodPage),
      userAnswers.getEither(BusinessNamePage)
    ).parMapN(AddedAsReportingNotificationRequest(_, _, isRPO, _, _, isExtendedDueDiligence, isActiveSellerDueDiligence))
  }
}

