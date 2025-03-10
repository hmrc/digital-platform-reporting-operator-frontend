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

import connectors.PlatformOperatorConnector
import controllers.AnswerExtractor
import controllers.actions._
import forms.RemovePlatformOperatorFormProvider
import models.audit.RemovePlatformOperatorAuditEventModel
import pages.update.{BusinessNamePage, PrimaryContactEmailPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{PlatformOperatorDeletedQuery, SentRemovedPlatformOperatorEmailQuery}
import repositories.SessionRepository
import services.{AuditService, EmailService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorSummaryViewModel
import views.html.update.RemovePlatformOperatorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemovePlatformOperatorController @Inject()(override val messagesApi: MessagesApi,
                                                 sessionRepository: SessionRepository,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalActionProvider,
                                                 requireData: DataRequiredAction,
                                                 formProvider: RemovePlatformOperatorFormProvider,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: RemovePlatformOperatorView,
                                                 platformConnector: PlatformOperatorConnector,
                                                 auditService: AuditService,
                                                 emailService: EmailService)
                                                (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswer(BusinessNamePage) { businessName =>
      Ok(view(formProvider(businessName), operatorId, businessName))
    }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async { implicit request =>
    getAnswerAsync(BusinessNamePage) { businessName =>
      formProvider(businessName).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, operatorId, businessName))),
        value => if (value) {
          for {
            _                <- platformConnector.removePlatformOperator(operatorId)
            emailsSentResult <- emailService.sendRemovePlatformOperatorEmails(request.userAnswers)
            poEmailAddress   = request.userAnswers.get(PrimaryContactEmailPage).get
            viewModel        = PlatformOperatorSummaryViewModel(operatorId, businessName, poEmailAddress)
            cleanedData      = request.userAnswers.copy(data = Json.obj())
            answers          <- Future.fromTry(cleanedData.set(PlatformOperatorDeletedQuery, viewModel))
            updatedAnswers   <- Future.fromTry(answers.set(SentRemovedPlatformOperatorEmailQuery, emailsSentResult))
            _ <- auditService.sendAudit(RemovePlatformOperatorAuditEventModel(businessName, operatorId).toAuditModel)
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(routes.PlatformOperatorRemovedController.onPageLoad(operatorId))
        } else {
          Future.successful(Redirect(routes.PlatformOperatorController.onPageLoad(operatorId)))
        }
      )
    }
  }
}
