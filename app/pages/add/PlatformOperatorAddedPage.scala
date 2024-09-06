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

package pages.add

import config.Service
import controllers.add.routes
import models.{NormalMode, UserAnswers}
import pages.Page
import play.api.Configuration
import play.api.mvc.Call

import javax.inject.Inject

class PlatformOperatorAddedPage @Inject()(configuration: Configuration) extends Page {

  private val manageService = configuration.get[Service]("microservice.services.digital-platform-reporting-manage-frontend")
  private val homePageUrl = s"${manageService.baseUrl}/manage-digital-platform-reporting"

  override protected def nextPageNormalMode(answers: UserAnswers): Call =
    Call("GET", homePageUrl)

  override protected def nextPageCheckMode(answers: UserAnswers): Call =
    Call("GET", homePageUrl)
}
