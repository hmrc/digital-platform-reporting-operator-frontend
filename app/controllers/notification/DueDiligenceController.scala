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
import forms.{DueDiligenceActiveOnlyFormProvider, DueDiligenceFormProvider}
import models.{Mode, UserAnswers}
import pages.notification.{DueDiligencePage, ReportingPeriodPage}
import pages.update.BusinessNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.notification.{DueDiligenceActiveOnlyView, DueDiligenceView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DueDiligenceController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalActionProvider,
                                        requireData: DataRequiredAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        formProvider: DueDiligenceFormProvider,
                                        formProviderActiveOnly: DueDiligenceActiveOnlyFormProvider,
                                        sessionRepository: SessionRepository,
                                        view: DueDiligenceView,
                                        viewActiveOnly: DueDiligenceActiveOnlyView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswer(BusinessNamePage) { businessName =>

      onlyAllowActiveSeller(request.userAnswers).map { activeOnly =>
        val form = if (activeOnly) formProviderActiveOnly(businessName) else formProvider(businessName)

        val preparedForm = request.userAnswers.get(DueDiligencePage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        if (activeOnly) {
          Ok(viewActiveOnly(preparedForm, mode, operatorId, businessName))
        } else {
          Ok(view(preparedForm, mode, operatorId, businessName))
        }
      }.getOrElse(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async {
    implicit request =>
      getAnswerAsync(BusinessNamePage) { businessName =>

        onlyAllowActiveSeller(request.userAnswers).map { activeOnly =>
          val form = if (activeOnly) formProviderActiveOnly(businessName) else formProvider(businessName)

          form.bindFromRequest().fold(
            formWithErrors => {
              if (activeOnly) {
                Future.successful(BadRequest(viewActiveOnly(formWithErrors, mode, operatorId, businessName)))
              } else {
                Future.successful(BadRequest(view(formWithErrors, mode, operatorId, businessName)))
              }
            },

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(DueDiligencePage, value))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(DueDiligencePage.nextPage(mode, operatorId, updatedAnswers))
          )
        }.getOrElse(Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad())))
      }
  }

  private def onlyAllowActiveSeller(answers: UserAnswers): Option[Boolean] =
    answers.get(ReportingPeriodPage).map { reportingPeriod =>
      answers.firstYearAsRpo.exists(_ < reportingPeriod)
    }
}
