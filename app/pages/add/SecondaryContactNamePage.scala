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

import controllers.add.routes
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case object SecondaryContactNamePage extends AddQuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "secondaryContactName"

  override protected def nextPageNormalMode(answers: UserAnswers): Call =
    routes.SecondaryContactEmailController.onPageLoad(NormalMode)

  override protected def nextPageCheckMode(answers: UserAnswers): Call =
    if (answers.get(SecondaryContactEmailPage).isDefined) {
      routes.CheckYourAnswersController.onPageLoad()
    } else {
      routes.SecondaryContactEmailController.onPageLoad(CheckMode)
    }

  override def route(mode: Mode): Call = routes.SecondaryContactNameController.onPageLoad(mode)
}
