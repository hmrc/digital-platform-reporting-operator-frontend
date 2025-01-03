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

import connectors.PlatformOperatorConnector
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import controllers.notification.routes.ViewNotificationsController
import controllers.{AnswerExtractor, routes => baseRoutes}
import forms.ViewNotificationsFormProvider
import models.NormalMode
import pages.notification.ViewNotificationsPage
import pages.update.BusinessNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{NotificationDetailsQuery, OriginalPlatformOperatorQuery}
import repositories.SessionRepository
import services.UserAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.notification.ViewNotificationsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewNotificationsController @Inject()(override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            formProvider: ViewNotificationsFormProvider,
                                            view: ViewNotificationsView,
                                            page: ViewNotificationsPage,
                                            platformOperatorConnector: PlatformOperatorConnector,
                                            userAnswersService: UserAnswersService,
                                            sessionRepository: SessionRepository)
                                           (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswers(NotificationDetailsQuery, BusinessNamePage) { case (notifications, businessName) =>
      Ok(view(formProvider(notifications.nonEmpty), notifications, operatorId, businessName))
    }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswers(NotificationDetailsQuery, BusinessNamePage) { case (notifications, businessName) =>
      formProvider(notifications.nonEmpty).bindFromRequest().fold(
        formWithErrors => BadRequest(view(formWithErrors, notifications, operatorId, businessName)),
        value => request.userAnswers.set(page, value).fold(
          _ => Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()),
          answers => Redirect(page.nextPage(NormalMode, operatorId, answers))
        )
      )
    }
  }

  def initialise(operatorId: String): Action[AnyContent] = identify.async { implicit request =>
    for {
      platformOperator <- platformOperatorConnector.viewPlatformOperator(operatorId)
      userAnswers <- Future.fromTry(userAnswersService.fromPlatformOperator(request.userId, platformOperator))
      updatedAnswers <- Future.fromTry(userAnswers.set(OriginalPlatformOperatorQuery, platformOperator))
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(ViewNotificationsController.onPageLoad(operatorId))
  }
}
