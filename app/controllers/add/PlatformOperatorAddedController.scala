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

package controllers.add

import controllers.actions._
import models.NormalMode
import pages.add.PlatformOperatorAddedPage

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorAddedQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.add.PlatformOperatorAddedView

class PlatformOperatorAddedController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalActionProvider,
                                                 requireData: DataRequiredAction,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: PlatformOperatorAddedView
                                               ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData(None) andThen requireData) { implicit request =>
    request.userAnswers.get(PlatformOperatorAddedQuery).map { viewModel =>
      Ok(view(viewModel))
    }.getOrElse {
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData(None) andThen requireData) { implicit request =>
    Redirect(PlatformOperatorAddedPage.nextPage(NormalMode, request.userAnswers))
  }
}