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

import com.google.inject.Inject
import connectors.PlatformOperatorConnector.CreatePlatformOperatorFailure
import connectors.{PlatformOperatorConnector, SubscriptionConnector}
import controllers.actions._
import models.audit.CreatePlatformOperatorAuditEventModel
import models.{NormalMode, UserAnswers}
import pages.add.{CheckYourAnswersPage, HasSecondaryContactPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorAddedQuery
import repositories.SessionRepository
import services.UserAnswersService.BuildCreatePlatformOperatorRequestFailure
import services.{AuditService, EmailService, UserAnswersService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorSummaryViewModel
import viewmodels.checkAnswers.add._
import viewmodels.govuk.summarylist._
import views.html.add.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            userAnswersService: UserAnswersService,
                                            connector: PlatformOperatorConnector,
                                            subscriptionConnector: SubscriptionConnector,
                                            sessionRepository: SessionRepository,
                                            auditService: AuditService,
                                            emailService: EmailService
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData(None) andThen requireData) {
    implicit request =>

      val platformOperatorList = SummaryListViewModel(
        rows = Seq(
          BusinessNameSummary.row(request.userAnswers),
          HasTradingNameSummary.row(request.userAnswers),
          TradingNameSummary.row(request.userAnswers),
          HasTaxIdentifierSummary.row(request.userAnswers),
          TaxResidentInUkSummary.row(request.userAnswers),
          UkTaxIdentifiersSummary.row(request.userAnswers),
          UtrSummary.row(request.userAnswers),
          CrnSummary.row(request.userAnswers),
          VrnSummary.row(request.userAnswers),
          EmprefSummary.row(request.userAnswers),
          ChrnSummary.row(request.userAnswers),
          TaxResidencyCountrySummary.row(request.userAnswers),
          InternationalTaxIdentifierSummary.row(request.userAnswers),
          RegisteredInUkSummary.row(request.userAnswers),
          UkAddressSummary.row(request.userAnswers),
          InternationalAddressSummary.row(request.userAnswers),
        ).flatten
      )

      Ok(view(platformOperatorList, primaryContactList(request.userAnswers), secondaryContactList(request.userAnswers)))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData(None) andThen requireData).async {
    implicit request =>

      userAnswersService.toCreatePlatformOperatorRequest(request.userAnswers, request.dprsId)
        .fold(
          errors => Future.failed(BuildCreatePlatformOperatorRequestFailure(errors)),
          createRequest =>
            (for {
              createResponse        <- connector.createPlatformOperator(createRequest)
              _                     <- auditService.sendAudit(CreatePlatformOperatorAuditEventModel(createRequest, createResponse).toAuditModel)
              cleanedAnswers        =  request.userAnswers.copy(data = Json.obj())
              platformOperatorInfo  =  PlatformOperatorSummaryViewModel(createResponse.operatorId, createRequest)
              updatedAnswers        <- Future.fromTry(cleanedAnswers.set(PlatformOperatorAddedQuery, platformOperatorInfo))
              _                     <- sessionRepository.set(updatedAnswers)
              subscriptionInfo      <- subscriptionConnector.getSubscriptionInfo
              answersWithOperatorId =  request.userAnswers.copy(operatorId = Some(createResponse.operatorId))
              _                     <- emailService.sendAddPlatformOperatorEmails(answersWithOperatorId, subscriptionInfo)
            } yield Redirect(CheckYourAnswersPage.nextPage(NormalMode, updatedAnswers))).recover {
              case error: CreatePlatformOperatorFailure => logger.warn("Failed to create platform operator", error)
                auditService.sendAudit(CreatePlatformOperatorAuditEventModel(createRequest, error.status).toAuditModel)
                throw error
              case error => logger.warn("Add platform operator emails not sent", error)
                throw error
            }
        )
  }

  private def primaryContactList(answers: UserAnswers)(implicit messages: Messages): SummaryList =
    if (answers.get(HasSecondaryContactPage).contains(false)) {
      SummaryListViewModel(
        rows = Seq(
          PrimaryContactNameSummary.row(answers),
          PrimaryContactEmailSummary.row(answers),
          CanPhonePrimaryContactSummary.row(answers),
          PrimaryContactPhoneNumberSummary.row(answers),
          HasSecondaryContactSummary.row(answers)
        ).flatten
      )
    } else {
      SummaryListViewModel(
        rows = Seq(
          PrimaryContactNameSummary.row(answers),
          PrimaryContactEmailSummary.row(answers),
          CanPhonePrimaryContactSummary.row(answers),
          PrimaryContactPhoneNumberSummary.row(answers)
        ).flatten
      )
    }

  private def secondaryContactList(answers: UserAnswers)(implicit messages: Messages): Option[SummaryList] =
    if (answers.get(HasSecondaryContactPage).contains(true)) {
      Some(SummaryListViewModel(
        rows = Seq(
          HasSecondaryContactSummary.row(answers),
          SecondaryContactNameSummary.row(answers),
          SecondaryContactEmailSummary.row(answers),
          CanPhoneSecondaryContactSummary.row(answers),
          SecondaryContactPhoneNumberSummary.row(answers),
        ).flatten
      ))
    } else {
      None
    }
}
