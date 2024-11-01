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
import models.{CheckMode, NormalMode, NotificationType, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case object ReportingPeriodPage extends NotificationQuestionPage[Int] {

  override protected def nextPageNormalMode(operatorId: String, answers: UserAnswers): Call =
    answers.get(NotificationTypePage).map {
      case NotificationType.Rpo => routes.DueDiligenceController.onPageLoad(NormalMode, operatorId)
      case NotificationType.Epo => routes.CheckYourAnswersController.onPageLoad(operatorId)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override protected def nextPageCheckMode(operatorId: String, answers: UserAnswers): Call =
    answers.get(NotificationTypePage).map {
      case NotificationType.Rpo =>
        answers.get(DueDiligencePage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(operatorId))
          .getOrElse(routes.DueDiligenceController.onPageLoad(CheckMode, operatorId))

      case NotificationType.Epo =>
        routes.CheckYourAnswersController.onPageLoad(operatorId)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def path: JsPath = JsPath \ "reportingPeriod"
}
