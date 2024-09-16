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
import controllers.{routes => baseRoutes}
import models.{CheckMode, DueDiligence, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case object DueDiligencePage extends NotificationQuestionPage[Set[DueDiligence]] {

  override protected def nextPageNormalMode(operatorId: String, answers: UserAnswers): Call =
    answers.get(this).map {
      case x if x.contains(DueDiligence.Extended) => routes.ReportingInFirstPeriodController.onPageLoad(NormalMode, operatorId)
      case _                                      => routes.CheckYourAnswersController.onPageLoad(operatorId)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override protected def nextPageCheckMode(operatorId: String, answers: UserAnswers): Call =
    answers.get(this).map {
      case x if x.contains(DueDiligence.Extended) =>
        answers.get(ReportingInFirstPeriodPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(operatorId))
          .getOrElse(routes.ReportingInFirstPeriodController.onPageLoad(CheckMode, operatorId))

      case _ => routes.CheckYourAnswersController.onPageLoad(operatorId)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def cleanup(value: Option[Set[DueDiligence]], userAnswers: UserAnswers): Try[UserAnswers] =
    value.map {
      case x if x.contains(DueDiligence.Extended) => super.cleanup(value, userAnswers)
      case _ => userAnswers.remove(ReportingInFirstPeriodPage)
    }.getOrElse(super.cleanup(value, userAnswers))

  override def path: JsPath = JsPath \ "dueDiligence"
}
