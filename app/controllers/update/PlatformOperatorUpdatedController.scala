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

package controllers.update

import controllers.actions._
import pages.add.{BusinessNamePage, PrimaryContactEmailPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorSummaryViewModel
import views.html.update.PlatformOperatorUpdatedView

import javax.inject.Inject

class PlatformOperatorUpdatedController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   identify: IdentifierAction,
                                                   getData: DataRetrievalActionProvider,
                                                   requireData: DataRequiredAction,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: PlatformOperatorUpdatedView
                                                 )
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    request.userAnswers.get(BusinessNamePage).map { businessName =>
      val viewModel = PlatformOperatorSummaryViewModel(operatorId, businessName, request.userAnswers.get(PrimaryContactEmailPage).toString)
      Ok(view(operatorId, viewModel))
    }.getOrElse {
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
