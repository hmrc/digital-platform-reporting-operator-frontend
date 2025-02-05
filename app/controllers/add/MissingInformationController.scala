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

package controllers.add

import controllers.actions._
import models.NormalMode
import models.operator.requests.CreatePlatformOperatorRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.add.MissingInformationView

import javax.inject.Inject
import scala.concurrent.Future

class MissingInformationController @Inject()(override val messagesApi: MessagesApi,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalActionProvider,
                                             requireData: DataRequiredAction,
                                             view: MissingInformationView,
                                             val controllerComponents: MessagesControllerComponents) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData(None) andThen requireData) { implicit request =>
    Ok(view())
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData(None) andThen requireData).async { implicit request =>
    CreatePlatformOperatorRequest.build(request.userAnswers, request.dprsId).fold(
      _ => Future.successful(Redirect(routes.BusinessNameController.onPageLoad(NormalMode).url)),
      _ => Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
    )
  }
}
