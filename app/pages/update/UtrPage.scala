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
import models.UkTaxIdentifiers._
import models.UserAnswers
import play.api.libs.json.JsPath
import play.api.mvc.Call

case object UtrPage extends UpdateQuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "utr"

  override def nextPage(operatorId: String, answers: UserAnswers): Call =
    answers.get(UkTaxIdentifiersPage).map { identifiers =>
      if (identifiers.contains(Crn) && answers.get(CrnPage).isEmpty) {
        routes.CrnController.onPageLoad(operatorId)
      } else if (identifiers.contains(Vrn) && answers.get(VrnPage).isEmpty) {
        routes.VrnController.onPageLoad(operatorId)
      } else if (identifiers.contains(Empref) && answers.get(EmprefPage).isEmpty) {
        routes.EmprefController.onPageLoad(operatorId)
      } else if (identifiers.contains(Chrn) && answers.get(ChrnPage).isEmpty) {
        routes.ChrnController.onPageLoad(operatorId)
      } else {
        routes.CheckYourAnswersController.onPageLoad(operatorId)
      }
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def route(operatorId: String): Call = routes.UtrController.onPageLoad(operatorId)
}
