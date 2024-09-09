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

import com.google.inject.Inject
import connectors.PlatformOperatorConnector
import controllers.actions._
import models.UserAnswers
import pages.update.{CheckYourAnswersPage, HasSecondaryContactPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorAddedQuery
import repositories.SessionRepository
import services.UserAnswersService
import services.UserAnswersService.BuildCreatePlatformOperatorRequestFailure
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorAddedViewModel
import viewmodels.checkAnswers.update._
import viewmodels.govuk.summarylist._
import views.html.update.CheckYourAnswersView

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
                                            sessionRepository: SessionRepository
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) {
    implicit request =>

      val platformOperatorList = SummaryListViewModel(
        rows = Seq(
          BusinessNameSummary.row(operatorId, request.userAnswers),
          HasTradingNameSummary.row(operatorId, request.userAnswers),
          TradingNameSummary.row(operatorId, request.userAnswers),
          TaxResidentInUkSummary.row(operatorId, request.userAnswers),
          HasUkTaxIdentifierSummary.row(operatorId, request.userAnswers),
          UkTaxIdentifiersSummary.row(operatorId, request.userAnswers),
          BusinessTypeSummary.row(operatorId, request.userAnswers),
          UtrSummary.row(operatorId, request.userAnswers),
          CrnSummary.row(operatorId, request.userAnswers),
          VrnSummary.row(operatorId, request.userAnswers),
          EmprefSummary.row(operatorId, request.userAnswers),
          ChrnSummary.row(operatorId, request.userAnswers),
          TaxResidencyCountrySummary.row(operatorId, request.userAnswers),
          HasInternationalTaxIdentifierSummary.row(operatorId, request.userAnswers),
          InternationalTaxIdentifierSummary.row(operatorId, request.userAnswers),
          RegisteredInUkSummary.row(operatorId, request.userAnswers),
          UkAddressSummary.row(operatorId, request.userAnswers),
          InternationalAddressSummary.row(operatorId, request.userAnswers),
        ).flatten
      )

      Ok(view(platformOperatorList, primaryContactList(operatorId, request.userAnswers), secondaryContactList(operatorId, request.userAnswers)))
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async {
    implicit request =>

      userAnswersService.toCreatePlatformOperatorRequest(request.userAnswers, request.dprsId)
        .fold(
          errors => Future.failed(BuildCreatePlatformOperatorRequestFailure(errors)),
          createRequest =>
            for {
              createResponse       <- connector.createPlatformOperator(createRequest)
              cleanedAnswers       =  request.userAnswers.copy(data = Json.obj())
              platformOperatorInfo =  PlatformOperatorAddedViewModel(createResponse.operatorId, createRequest)
              updatedAnswers       <- Future.fromTry(cleanedAnswers.set(PlatformOperatorAddedQuery, platformOperatorInfo))
              _                    <- sessionRepository.set(updatedAnswers)
            } yield Redirect(CheckYourAnswersPage.nextPage(operatorId, updatedAnswers))
        )
  }

  private def primaryContactList(operatorId: String, answers: UserAnswers)(implicit messages: Messages): SummaryList =
    if (answers.get(HasSecondaryContactPage).contains(false)) {
      SummaryListViewModel(
        rows = Seq(
          PrimaryContactNameSummary.row(operatorId, answers),
          PrimaryContactEmailSummary.row(operatorId, answers),
          CanPhonePrimaryContactSummary.row(operatorId, answers),
          PrimaryContactPhoneNumberSummary.row(operatorId, answers),
          HasSecondaryContactSummary.row(operatorId, answers)
        ).flatten
      )
    } else {
      SummaryListViewModel(
        rows = Seq(
          PrimaryContactNameSummary.row(operatorId, answers),
          PrimaryContactEmailSummary.row(operatorId, answers),
          CanPhonePrimaryContactSummary.row(operatorId, answers),
          PrimaryContactPhoneNumberSummary.row(operatorId, answers)
        ).flatten
      )
    }

  private def secondaryContactList(operatorId: String, answers: UserAnswers)(implicit messages: Messages): Option[SummaryList] =
    if(answers.get(HasSecondaryContactPage).contains(true)) {
      Some(SummaryListViewModel(
        rows = Seq(
          HasSecondaryContactSummary.row(operatorId, answers),
          SecondaryContactNameSummary.row(operatorId, answers),
          SecondaryContactEmailSummary.row(operatorId, answers),
          CanPhoneSecondaryContactSummary.row(operatorId, answers),
          SecondaryContactPhoneNumberSummary.row(operatorId, answers),
        ).flatten
      ))
    } else {
      None
    }
}