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
import config.FrontendAppConfig
import models.{NormalMode, UserAnswers}
import org.mockito.Mockito.when
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers.GET

class ViewNotificationsPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues with MockitoSugar {

  private val operatorId = "operatorId"
  private val mockConfig = mock[FrontendAppConfig]
  private val emptyAnswers = UserAnswers("id")
  val page = new ViewNotificationsPage(mockConfig)

  when(mockConfig.manageFrontendUrl) thenReturn "/foo"

  ".nextPage" - {

    "must go to AddGuidance when the answer is yes" in {

      val answers = emptyAnswers.set(page, true).success.value
      page.nextPage(NormalMode, operatorId, answers) mustEqual routes.AddGuidanceController.onPageLoad(operatorId)
    }

    "must go to the manage frontend when the answer is no" in {

      val answers = emptyAnswers.set(page, false).success.value
      page.nextPage(NormalMode, operatorId, answers) mustEqual Call(GET, "/foo")
    }
  }
}
