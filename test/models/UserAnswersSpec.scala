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

import models.operator.NotificationType.{Epo, Rpo}
import models.operator.responses.NotificationDetails
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import queries.NotificationDetailsQuery

import java.time.Instant

class UserAnswersSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

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
