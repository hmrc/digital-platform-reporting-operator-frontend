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

case object TaxResidentInUkPage extends UpdateQuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "taxResidentInUk"

  override def nextPage(operatorId: String, answers: UserAnswers): Call =
    answers.get(this).map {
      case true =>
        answers.get(UkTaxIdentifiersPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(operatorId))
          .getOrElse(routes.UkTaxIdentifiersController.onPageLoad(operatorId))

      case false =>
        answers.get(TaxResidencyCountryPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(operatorId))
          .getOrElse(routes.TaxResidencyCountryController.onPageLoad(operatorId))
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    value.map {
      case true =>
        userAnswers
          .remove(TaxResidencyCountryPage)
          .flatMap(_.remove(InternationalTaxIdentifierPage))

      case false =>
        userAnswers
        .remove(UkTaxIdentifiersPage)
          .flatMap(_.remove(UtrPage))
          .flatMap(_.remove(CrnPage))
          .flatMap(_.remove(VrnPage))
          .flatMap(_.remove(EmprefPage))
          .flatMap(_.remove(ChrnPage))
    }.getOrElse(super.cleanup(value, userAnswers))
}
