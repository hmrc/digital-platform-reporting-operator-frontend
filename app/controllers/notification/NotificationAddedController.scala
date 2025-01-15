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

import connectors.SubscriptionConnector
import controllers.AnswerExtractor
import controllers.actions._
import pages.add.{BusinessNamePage, PrimaryContactEmailPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.SentAddedReportingNotificationEmailQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.notification._
import viewmodels.govuk.summarylist._
import views.html.notification.NotificationAddedView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class NotificationAddedController @Inject()(override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: NotificationAddedView,
                                            connector: SubscriptionConnector)
                                           (implicit executionContext: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async { implicit request =>
    getAnswerAsync(BusinessNamePage) { businessName =>
      val list = SummaryListViewModel(
        rows = Seq(
          OperatorNameSummary.summaryRow(request.userAnswers),
          OperatorIdSummary.summaryRow(request.userAnswers),
          NotificationTypeSummary.summaryRow(request.userAnswers),
          ReportingPeriodSummary.summaryRow(request.userAnswers),
          DueDiligenceSummary.summaryRow(request.userAnswers)
        ).flatten
      )
      val poContactEmail = request.userAnswers.get(PrimaryContactEmailPage).getOrElse("")
      val emailSent = request.userAnswers.get(SentAddedReportingNotificationEmailQuery).getOrElse(false)
      connector.getSubscriptionInfo.map { x => Ok(view(operatorId, businessName, list, x.primaryContact.email, poContactEmail, emailSent)) }
    }
  }
}
