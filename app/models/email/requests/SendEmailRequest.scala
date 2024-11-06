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
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}
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

  def build(userAnswers: UserAnswers): EitherNec[Query, AddedPlatformOperatorRequest] = (
    userAnswers.getEither(PrimaryContactEmailPage),
    userAnswers.getEither(PrimaryContactNamePage),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(AddedPlatformOperatorRequest(_, _, _, userAnswers.operatorId.get))
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

  // TODO need to correct these
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

  def build(userAnswers: UserAnswers): EitherNec[Query, RemovedPlatformOperatorRequest] = (
    userAnswers.getEither(PrimaryContactEmailPage),
    userAnswers.getEither(PrimaryContactNamePage),
    userAnswers.getEither(BusinessNamePage)
  ).parMapN(RemovedPlatformOperatorRequest(_, _, _, userAnswers.operatorId.get))
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
