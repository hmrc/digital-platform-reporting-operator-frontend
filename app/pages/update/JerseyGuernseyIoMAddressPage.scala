/*
 * Copyright 2025 HM Revenue & Customs
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
import models.{JerseyGuernseyIoMAddress, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case object JerseyGuernseyIoMAddressPage extends UpdateQuestionPage[JerseyGuernseyIoMAddress] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "jerseyGuernseyIoMAddress"

  override def cleanup(value: Option[JerseyGuernseyIoMAddress], userAnswers: UserAnswers): Try[UserAnswers] =
    userAnswers.get(JerseyGuernseyIoMAddressPage)
      .map(_ => userAnswers.remove(UkAddressPage))
      .getOrElse(super.cleanup(value, userAnswers))

  override def route(operatorId: String): Call = routes.JerseyGuernseyIoMAddressController.onPageLoad(operatorId)
}
