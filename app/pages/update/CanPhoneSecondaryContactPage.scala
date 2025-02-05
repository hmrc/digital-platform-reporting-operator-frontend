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

package pages.update

import controllers.update.routes
import controllers.{routes => baseRoutes}
import models.UserAnswers
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case object CanPhoneSecondaryContactPage extends UpdateQuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "canPhoneSecondaryContact"

  override def nextPage(operatorId: String, answers: UserAnswers): Call = answers.get(this).map {
    case true => answers.get(SecondaryContactPhoneNumberPage)
      .map(_ => routes.CheckYourAnswersController.onPageLoad(operatorId))
      .getOrElse(routes.SecondaryContactPhoneNumberController.onPageLoad(operatorId))
    case false => routes.CheckYourAnswersController.onPageLoad(operatorId)
  }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    if (value.contains(false)) userAnswers.remove(SecondaryContactPhoneNumberPage) else super.cleanup(value, userAnswers)

  override def route(operatorId: String): Call = routes.CanPhoneSecondaryContactController.onPageLoad(operatorId)
}
