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
import models.{CheckMode, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case object HasTaxIdentifierPage extends AddQuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "hasTaxIdentifier"

  override protected def nextPageNormalMode(answers: UserAnswers): Call =
    answers.get(this).map {
      case true  => routes.TaxResidentInUkController.onPageLoad(NormalMode)
      case false => routes.RegisteredInUkController.onPageLoad(NormalMode)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override protected def nextPageCheckMode(answers: UserAnswers): Call =
    answers.get(this).map {
      case true =>
        answers.get(TaxResidentInUkPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad())
          .getOrElse(routes.TaxResidentInUkController.onPageLoad(CheckMode))

      case false =>
        routes.CheckYourAnswersController.onPageLoad()
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    if (value.contains(false)) {
      userAnswers
        .remove(TaxResidentInUkPage)
        .flatMap(_.remove(UkTaxIdentifiersPage))
        .flatMap(_.remove(BusinessTypePage))
        .flatMap(_.remove(UtrPage))
        .flatMap(_.remove(CrnPage))
        .flatMap(_.remove(VrnPage))
        .flatMap(_.remove(EmprefPage))
        .flatMap(_.remove(ChrnPage))
        .flatMap(_.remove(TaxResidencyCountryPage))
        .flatMap(_.remove(InternationalTaxIdentifierPage))
    } else {
      super.cleanup(value, userAnswers)
    }
}