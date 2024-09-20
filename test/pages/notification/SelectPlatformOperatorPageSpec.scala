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

package pages.notification

import controllers.notification.routes
import models.operator.NotificationType
import models.operator.responses.NotificationDetails
import models.{NormalMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import queries.NotificationDetailsQuery

import java.time.Instant

class SelectPlatformOperatorPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")

    "must go to Add Guidance for the correct platform operator when there are no existing notifications" in {

      SelectPlatformOperatorPage.nextPage(NormalMode, "operatorId", emptyAnswers)
        .mustEqual(routes.AddGuidanceController.onPageLoad("operatorId"))
    }

    "must go to View Notifications for the correct platform operator when there is at least one existing notifications" in {

      val notification = NotificationDetails(NotificationType.Epo, None, None, 2024, Instant.now)
      val answers = emptyAnswers.set(NotificationDetailsQuery, Seq(notification)).success.value

      SelectPlatformOperatorPage.nextPage(NormalMode, "operatorId", answers)
        .mustEqual(routes.ViewNotificationsController.onPageLoad("operatorId"))
    }
  }
}
