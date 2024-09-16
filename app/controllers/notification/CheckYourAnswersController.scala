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

import com.google.inject.Inject
import connectors.PlatformOperatorConnector
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import models.NormalMode
import pages.notification.CheckYourAnswersPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UserAnswersService
import services.UserAnswersService.BuildAddNotificationRequestFailure
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.notification._
import viewmodels.govuk.summarylist._
import views.html.notification.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            userAnswersService: UserAnswersService,
                                            connector: PlatformOperatorConnector
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) {
    implicit request =>

      val list = SummaryListViewModel(
        rows = Seq(
          NotificationTypeSummary.row(operatorId, request.userAnswers),
          ReportingPeriodSummary.row(operatorId, request.userAnswers),
          DueDiligenceSummary.row(operatorId, request.userAnswers),
          ReportingInFirstPeriodSummary.row(operatorId, request.userAnswers)
        ).flatten
      )

      Ok(view(list, operatorId))
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async {
    implicit request =>
      userAnswersService.addNotificationRequest(request.userAnswers, request.dprsId, operatorId)
        .fold(
          errors => Future.failed(BuildAddNotificationRequestFailure(errors)),
          addNotificationRequest =>
            connector
              .updatePlatformOperator(addNotificationRequest)
              .map(_ => Redirect(CheckYourAnswersPage.nextPage(NormalMode, operatorId, request.userAnswers)))
        )
  }
}
