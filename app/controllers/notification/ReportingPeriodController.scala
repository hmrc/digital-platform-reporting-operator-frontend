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
import controllers.actions._
import forms.ReportingPeriodFormProvider
import models.Mode
import pages.notification.{NotificationTypePage, ReportingPeriodPage}
import pages.update.BusinessNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.NotificationDetailsQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.notification.{ReportingPeriodFirstView, ReportingPeriodView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingPeriodController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           identify: IdentifierAction,
                                           getData: DataRetrievalActionProvider,
                                           requireData: DataRequiredAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           formProvider: ReportingPeriodFormProvider,
                                           sessionRepository: SessionRepository,
                                           view: ReportingPeriodView,
                                           viewFirst: ReportingPeriodFirstView
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    {
      for {
        businessName     <- request.userAnswers.get(BusinessNamePage)
        notifications    <- request.userAnswers.get(NotificationDetailsQuery)
        notificationType <- request.userAnswers.get(NotificationTypePage)
      } yield {
        val form = formProvider(businessName)

        val preparedForm = request.userAnswers.get(ReportingPeriodPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        if (notifications.isEmpty) {
          Ok(viewFirst(preparedForm, mode, operatorId, businessName, notificationType))
        } else {
          Ok(view(preparedForm, mode, operatorId, businessName))
        }
      }
    }.getOrElse(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()))
  }

  def onSubmit(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async {
    implicit request => {
      for {
        businessName <- request.userAnswers.get(BusinessNamePage)
        notifications <- request.userAnswers.get(NotificationDetailsQuery)
        notificationType <- request.userAnswers.get(NotificationTypePage)
      } yield {

        val form = formProvider(businessName)

        form.bindFromRequest().fold(
          formWithErrors => {
            if (notifications.isEmpty) {
              Future.successful(BadRequest(viewFirst(formWithErrors, mode, operatorId, businessName, notificationType)))
            } else {
              Future.successful(BadRequest(view(formWithErrors, mode, operatorId, businessName)))
            }
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ReportingPeriodPage, value))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(ReportingPeriodPage.nextPage(mode, operatorId, updatedAnswers))
        )
      }
    }.getOrElse(Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad())))
  }
}
