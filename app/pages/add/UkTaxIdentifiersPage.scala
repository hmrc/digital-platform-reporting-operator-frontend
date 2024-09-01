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
import controllers.{routes => baseRoutes}
import models.UkTaxIdentifiers._
import models.{CheckMode, NormalMode, UkTaxIdentifiers, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.{Success, Try}

case object UkTaxIdentifiersPage extends AddQuestionPage[Set[UkTaxIdentifiers]] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "ukTaxIdentifiers"

  override protected def nextPageNormalMode(answers: UserAnswers): Call =
    answers.get(this).map {
      case x if x.contains(Utr)    => routes.BusinessTypeController.onPageLoad(NormalMode)
      case x if x.contains(Crn)    => routes.CrnController.onPageLoad(NormalMode)
      case x if x.contains(Vrn)    => routes.VrnController.onPageLoad(NormalMode)
      case x if x.contains(Empref) => routes.EmprefController.onPageLoad(NormalMode)
      case _                       => routes.ChrnController.onPageLoad(NormalMode)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override protected def nextPageCheckMode(answers: UserAnswers): Call =
    answers.get(this).map { identifiers =>
      if (identifiers.contains(Utr) && answers.get(BusinessTypePage).isEmpty) {
        routes.BusinessTypeController.onPageLoad(CheckMode)
      } else if (identifiers.contains(Crn) && answers.get(CrnPage).isEmpty) {
        routes.CrnController.onPageLoad(CheckMode)
      } else if (identifiers.contains(Vrn) && answers.get(VrnPage).isEmpty) {
        routes.VrnController.onPageLoad(CheckMode)
      } else if (identifiers.contains(Empref) && answers.get(EmprefPage).isEmpty) {
        routes.EmprefController.onPageLoad(CheckMode)
      } else if (identifiers.contains(Chrn) && answers.get(ChrnPage).isEmpty) {
        routes.ChrnController.onPageLoad(CheckMode)
      } else {
        routes.CheckYourAnswersController.onPageLoad()
      }
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def cleanup(value: Option[Set[UkTaxIdentifiers]], userAnswers: UserAnswers): Try[UserAnswers] = {
    value.map { identifiers =>
      maybeRemoveUtr(userAnswers, identifiers)
        .flatMap(maybeRemoveCrn(_, identifiers))
        .flatMap(maybeRemoveVrn(_, identifiers))
        .flatMap(maybeRemoveEmpref(_, identifiers))
        .flatMap(maybeRemoveChrn(_, identifiers))
    }.getOrElse(super.cleanup(value, userAnswers))
  }

  private def maybeRemoveUtr(answers: UserAnswers, identifiers: Set[UkTaxIdentifiers]): Try[UserAnswers] =
    if (identifiers.contains(Utr)) {
      Success(answers)
    } else {
      answers.remove(BusinessTypePage).flatMap(_.remove(UtrPage))
    }

  private def maybeRemoveCrn(answers: UserAnswers, identifiers: Set[UkTaxIdentifiers]): Try[UserAnswers] =
    if (identifiers.contains(Crn)) Success(answers) else answers.remove(CrnPage)

  private def maybeRemoveVrn(answers: UserAnswers, identifiers: Set[UkTaxIdentifiers]): Try[UserAnswers] =
    if (identifiers.contains(Vrn)) Success(answers) else answers.remove(VrnPage)

  private def maybeRemoveEmpref(answers: UserAnswers, identifiers: Set[UkTaxIdentifiers]): Try[UserAnswers] =
    if (identifiers.contains(Empref)) Success(answers) else answers.remove(EmprefPage)

  private def maybeRemoveChrn(answers: UserAnswers, identifiers: Set[UkTaxIdentifiers]): Try[UserAnswers] =
    if (identifiers.contains(Chrn)) Success(answers) else answers.remove(ChrnPage)
}
