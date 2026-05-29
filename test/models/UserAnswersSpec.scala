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

import models.Country.UnitedKingdom
import models.operator.NotificationType.{Epo, Rpo}
import models.operator.responses.NotificationDetails
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.QuestionPage
import play.api.libs.json.{JsArray, JsBoolean, JsObject, JsPath, JsString, Json}
import queries.NotificationDetailsQuery

import java.time.Instant

class UserAnswersSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private object BooleanPage extends QuestionPage[Boolean] {
    override def path: JsPath = JsPath \ "boolean"
  }

  private object StringPage extends QuestionPage[String] {
    override def path: JsPath = JsPath \ "string"
  }

  ".get" - {

    "must read boolean values stored as strings" in {

      val answers = UserAnswers("id", data = Json.obj("boolean" -> "false"))

      answers.get(BooleanPage).value mustEqual false
    }

    "must preserve string values that look like booleans" in {

      val answers = UserAnswers("id", data = Json.obj("string" -> "false"))

      answers.get(StringPage).value mustEqual "false"
    }
  }

  ".set" - {
    "must write non-enumerable values with their own Writes when enumerable implicits are imported" in {

      import UkTaxIdentifiers._

      val address = UkAddress("line 1", Some("line 2"), "town", Some("county"), "AA1 1AA", UnitedKingdom)
      val answers = UserAnswers("id")
        .set(BooleanPage, false).success.value
        .set(pages.add.UkAddressPage, address).success.value
        .set(pages.add.UkTaxIdentifiersPage, Set(Utr)).success.value

      (answers.data \ "boolean").asOpt[JsBoolean].value mustEqual JsBoolean(false)
      (answers.data \ "ukAddress").asOpt[JsObject].value
      (answers.data \ "ukTaxIdentifiers").asOpt[JsArray].value mustEqual JsArray(Seq(JsString("utr")))
      answers.get(pages.add.UkAddressPage).value mustEqual address
    }
  }

  ".firstYearAsRpo" - {

    val emptyAnswers = UserAnswers("id")
    val instant = Instant.parse("2024-12-31T00:00:00Z")

    "must be None when there are no notifications" in {

      emptyAnswers.firstYearAsRpo must not be defined
    }

    "must be None when the list of notifications is empty" in {

      val answers = emptyAnswers.set(NotificationDetailsQuery, Nil).success.value

      answers.firstYearAsRpo must not be defined
    }

    "must be None when all notifications are as an EPO" in {

      val notifications = Seq(
        NotificationDetails(Epo, None, None, 2024, instant),
        NotificationDetails(Epo, None, None, 2025, instant),
        NotificationDetails(Epo, None, None, 2026, instant)
      )

      val answers = emptyAnswers.set(NotificationDetailsQuery, notifications).success.value

      answers.firstYearAsRpo must not be defined
    }

    "must be the lowest reporting period which has an RPO as the active record" - {

      val notifications = Seq(
        NotificationDetails(Epo, None, None, 2024, instant),
        NotificationDetails(Rpo, None, None, 2025, instant),
        NotificationDetails(Epo, None, None, 2025, instant.plusSeconds(1)),
        NotificationDetails(Epo, None, None, 2026, instant),
        NotificationDetails(Rpo, None, None, 2026, instant.plusSeconds(1)),
        NotificationDetails(Rpo, None, None, 2027, instant)
      )

      val answers = emptyAnswers.set(NotificationDetailsQuery, notifications).success.value

      answers.firstYearAsRpo.value mustEqual 2026
    }
  }
}
