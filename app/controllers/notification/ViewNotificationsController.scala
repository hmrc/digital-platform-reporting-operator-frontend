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

package controllers.notification

import controllers.{routes => baseRoutes}
import controllers.AnswerExtractor
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import forms.ViewNotificationsFormProvider
import models.NormalMode
import pages.notification.ViewNotificationsPage
import pages.update.BusinessNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.NotificationDetailsQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.notification.ViewNotificationsView

import javax.inject.Inject

class ViewNotificationsController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalActionProvider,
                                             requireData: DataRequiredAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             formProvider: ViewNotificationsFormProvider,
                                             view: ViewNotificationsView,
                                             page: ViewNotificationsPage
                                           ) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswers(NotificationDetailsQuery, BusinessNamePage) { case (notifications, businessName) =>

      val form = formProvider(notifications.nonEmpty)

      Ok(view(form, notifications, operatorId, businessName))
    }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswers(NotificationDetailsQuery, BusinessNamePage) { case (notifications, businessName) =>

      val form = formProvider(notifications.nonEmpty)

      form.bindFromRequest().fold(
        formWithErrors => BadRequest(view(formWithErrors, notifications, operatorId, businessName)),
        value => request.userAnswers.set(page, value).fold(
          _       => Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()),
          answers => Redirect(page.nextPage(NormalMode, operatorId, answers))
        )
      )
    }
  }
}
