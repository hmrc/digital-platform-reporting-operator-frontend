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

package models

import cats.data.{EitherNec, NonEmptyChain}
import models.operator.NotificationType.Rpo
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import queries.{Gettable, NotificationDetailsQuery, Query, Settable}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
                              userId: String,
                              operatorId: Option[String] = None,
                              data: JsObject = Json.obj(),
                              lastUpdated: Instant = Instant.now
                            ) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def getEither[A](page: Gettable[A])(implicit rds: Reads[A]): EitherNec[Query, A] =
    get(page).toRight(NonEmptyChain.one(page))

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap {
      d =>
        val updatedAnswers = copy (data = d)
        page.cleanup(Some(value), updatedAnswers)
    }
  }

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_) =>
        Success(data)
    }

    updatedData.flatMap {
      d =>
        val updatedAnswers = copy (data = d)
        page.cleanup(None, updatedAnswers)
    }
  }

  lazy val firstYearAsRpo: Option[Int] =
    get(NotificationDetailsQuery).flatMap(
      _.groupBy(_.firstPeriod)
        .map(_._2.maxBy(_.receivedDateTime))
        .filter(_.notificationType == Rpo)
        .map(_.firstPeriod)
        .toSeq
        .minOption
    )
}

object UserAnswers {

  def encryptedFormat(implicit crypto: Encrypter with Decrypter): OFormat[UserAnswers] = {
    implicit val sensitiveFormat: Format[SensitiveString] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

    val encryptedReads: Reads[UserAnswers] =
      (
        (__ \ "userId").read[String] and
          (__ \ "operatorId").readNullable[String] and
          (__ \ "data").read[SensitiveString] and
          (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
        )((userId, operatorId, data, lastUpdated) =>
        UserAnswers(userId, operatorId, Json.parse(data.decryptedValue).as[JsObject], lastUpdated)
      )

    val encryptedWrites: OWrites[UserAnswers] =
      (
        (__ \ "userId").write[String] and
          (__ \ "operatorId").writeNullable[String] and
          (__ \ "data").write[SensitiveString] and
          (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
        )(ua => (ua.userId, ua.operatorId, SensitiveString(Json.stringify(ua.data)), ua.lastUpdated))

    OFormat(encryptedReads, encryptedWrites)
  }

  val reads: Reads[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "userId").read[String] and
      (__ \ "operatorId").readNullable[String] and
      (__ \ "data").read[JsObject] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    ) (UserAnswers.apply _)
  }

  val writes: OWrites[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "userId").write[String] and
      (__ \ "operatorId").writeNullable[String] and
      (__ \ "data").write[JsObject] and
      (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    ) (ua => (ua.userId, ua.operatorId, ua.data, ua.lastUpdated))
  }

  implicit val format: OFormat[UserAnswers] = OFormat(reads, writes)
}
