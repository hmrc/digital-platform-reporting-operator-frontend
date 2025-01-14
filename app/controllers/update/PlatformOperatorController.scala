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

import connectors.{PlatformOperatorConnector, SubmissionsConnector}
import controllers.actions._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.OriginalPlatformOperatorQuery
import repositories.SessionRepository
import services.UserAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorViewModel
import views.html.update.PlatformOperatorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PlatformOperatorController @Inject()(override val messagesApi: MessagesApi,
                                           identify: IdentifierAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: PlatformOperatorView,
                                           platformOperatorConnector: PlatformOperatorConnector,
                                           submissionsConnector: SubmissionsConnector,
                                           sessionRepository: SessionRepository,
                                           userAnswersService: UserAnswersService)
                                          (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = identify.async { implicit request =>
    for {
      platformOperator <- platformOperatorConnector.viewPlatformOperator(operatorId)
      hasSubmissions <- submissionsConnector.submissionsExist(operatorId)
      hasAssumedReports <- submissionsConnector.assumedReportsExist(operatorId)
      userAnswers <- Future.fromTry(userAnswersService.fromPlatformOperator(request.userId, platformOperator))
      updatedAnswers <- Future.fromTry(userAnswers.set(OriginalPlatformOperatorQuery, platformOperator))
      _ <- sessionRepository.set(updatedAnswers)
      viewModel = PlatformOperatorViewModel(platformOperator, hasSubmissions, hasAssumedReports)
    } yield Ok(view(viewModel))
  }
}
