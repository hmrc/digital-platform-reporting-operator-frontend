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
import models.RegisteredAddressCountry.{International, JerseyGuernseyIsleOfMan, Uk}
import models.{CheckMode, Mode, NormalMode, RegisteredAddressCountry, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case object RegisteredInUkPage extends AddQuestionPage[RegisteredAddressCountry] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "registeredInUk"

  override protected def nextPageNormalMode(answers: UserAnswers): Call =
    answers.get(this).map {
      case Uk                      => routes.UkAddressController.onPageLoad(NormalMode)
      case JerseyGuernseyIsleOfMan => routes.JerseyGuernseyIoMAddressController.onPageLoad(NormalMode)
      case International           => routes.InternationalAddressController.onPageLoad(NormalMode)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override protected def nextPageCheckMode(answers: UserAnswers): Call =
    answers.get(this).map {
      case Uk =>
        answers.get(UkAddressPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad())
          .getOrElse(routes.UkAddressController.onPageLoad(CheckMode))

      case JerseyGuernseyIsleOfMan =>
        answers.get(JerseyGuernseyIoMAddressPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad())
          .getOrElse(routes.JerseyGuernseyIoMAddressController.onPageLoad(CheckMode))

      case International =>
        answers.get(InternationalAddressPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad())
          .getOrElse(routes.InternationalAddressController.onPageLoad(CheckMode))
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def cleanup(value: Option[RegisteredAddressCountry], userAnswers: UserAnswers): Try[UserAnswers] = {
  value.map {
    case Uk                      => userAnswers.remove(JerseyGuernseyIoMAddressPage).flatMap(_.remove(InternationalAddressPage))
    case JerseyGuernseyIsleOfMan => userAnswers.remove(UkAddressPage).flatMap(_.remove(InternationalAddressPage))
    case International           => userAnswers.remove(UkAddressPage).flatMap(_.remove(JerseyGuernseyIoMAddressPage))
  }.getOrElse(super.cleanup(value, userAnswers))}

  override def route(mode: Mode): Call = routes.RegisteredInUkController.onPageLoad(mode)
}
