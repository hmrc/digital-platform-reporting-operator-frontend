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
import models.{RegisteredAddressCountry, UserAnswers}
import RegisteredAddressCountry.{International, JerseyGuernseyIsleOfMan, Uk}
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case object RegisteredInUkPage extends UpdateQuestionPage[RegisteredAddressCountry] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "registeredInUk"

  override def nextPage(operatorId: String, answers: UserAnswers): Call =
    answers.get(this).map {
      case Uk =>
        answers.get(UkAddressPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(operatorId))
          .getOrElse(routes.UkAddressController.onPageLoad(operatorId))

      case JerseyGuernseyIsleOfMan =>
        answers.get(JerseyGuernseyIoMAddressPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(operatorId))
          .getOrElse(routes.JerseyGuernseyIoMAddressController.onPageLoad(operatorId))


      case International =>
        answers.get(InternationalAddressPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(operatorId))
          .getOrElse(routes.InternationalAddressController.onPageLoad(operatorId))
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def cleanup(value: Option[RegisteredAddressCountry], userAnswers: UserAnswers): Try[UserAnswers] = {
    value.map {
      case Uk                      => userAnswers.remove(JerseyGuernseyIoMAddressPage).flatMap(_.remove(InternationalAddressPage))
      case JerseyGuernseyIsleOfMan => userAnswers.remove(UkAddressPage).flatMap(_.remove(InternationalAddressPage))
      case International           => userAnswers.remove(UkAddressPage).flatMap(_.remove(JerseyGuernseyIoMAddressPage))
    }.getOrElse(super.cleanup(value, userAnswers))}
}
