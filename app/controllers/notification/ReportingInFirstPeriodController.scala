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

import controllers.AnswerExtractor
import controllers.actions._
import forms.ReportingInFirstPeriodFormProvider
import models.Mode
import pages.notification.{ReportingInFirstPeriodPage, ReportingPeriodPage}
import pages.update.BusinessNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.notification.ReportingInFirstPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingInFirstPeriodController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  identify: IdentifierAction,
                                                  getData: DataRetrievalActionProvider,
                                                  requireData: DataRequiredAction,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  formProvider: ReportingInFirstPeriodFormProvider,
                                                  sessionRepository: SessionRepository,
                                                  view: ReportingInFirstPeriodView
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswers(BusinessNamePage, ReportingPeriodPage) { case (businessName, reportingPeriod) =>

      val form = formProvider(businessName, reportingPeriod)

      val preparedForm = request.userAnswers.get(ReportingInFirstPeriodPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, operatorId, businessName, reportingPeriod))
    }
  }

  def onSubmit(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async { implicit request =>
    getAnswersAsync(BusinessNamePage, ReportingPeriodPage) { case (businessName, reportingPeriod) =>

      val form = formProvider(businessName, reportingPeriod)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, operatorId, businessName, reportingPeriod))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ReportingInFirstPeriodPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(ReportingInFirstPeriodPage.nextPage(mode, operatorId, updatedAnswers))
      )
    }
  }
}
