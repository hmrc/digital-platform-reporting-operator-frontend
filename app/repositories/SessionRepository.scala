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

package repositories

import config.FrontendAppConfig
import models.UserAnswers
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.SingleObservableFuture
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionRepository @Inject()(
                                   mongoComponent: MongoComponent,
                                   appConfig: FrontendAppConfig,
                                   clock: Clock
                                 )(implicit ec: ExecutionContext, crypto: Encrypter with Decrypter)
  extends PlayMongoRepository[UserAnswers](
    collectionName = "user-answers",
    mongoComponent = mongoComponent,
    domainFormat = if (appConfig.dataEncryptionEnabled) UserAnswers.encryptedFormat else UserAnswers.format,
    indexes        = Seq(
      IndexModel(
        Indexes.ascending("userId", "operatorId"),
        IndexOptions()
          .name("idIdx")
          .unique(true)
          .sparse(true)
      ),
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
      )
    )
  ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byUserId(userId: String): Bson = Filters.equal("userId", userId)

  private def byIds(userId: String, operatorId: Option[String]): Bson = {
    Filters.and(
      Filters.equal("userId", userId),
      operatorId.map(Filters.equal("operatorId", _)).getOrElse(Filters.exists("operatorId", exists = false))
    )
  }

  def keepAlive(userId: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .updateMany(
        filter = byUserId(userId),
        update = Updates.set("lastUpdated", Instant.now(clock)),
      )
      .toFuture()
      .map(_ => true)
  }

  def get(userId: String, operatorId: Option[String]): Future[Option[UserAnswers]] = Mdc.preservingMdc {
    keepAlive(userId).flatMap {
      _ =>
        collection
          .find(byIds(userId, operatorId))
          .headOption()
    }
  }

  def set(answers: UserAnswers): Future[Boolean] = Mdc.preservingMdc {

    val updatedAnswers = answers copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter      = byIds(updatedAnswers.userId, updatedAnswers.operatorId),
        replacement = updatedAnswers,
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }

  def clear(userId: String, operatorId: Option[String]): Future[Boolean] = Mdc.preservingMdc {
    collection
      .deleteOne(byIds(userId, operatorId))
      .toFuture()
      .map(_ => true)
  }

  def clear(userId: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .deleteMany(byUserId(userId))
      .toFuture()
      .map(_ => true)
  }
}
