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
import controllers.actions._
import models.NormalMode
import pages.notification.AddGuidancePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.UserAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.notification.AddGuidanceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddGuidanceController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalActionProvider,
                                       requireData: DataRequiredAction,
                                       platformOperatorConnector: PlatformOperatorConnector,
                                       userAnswersService: UserAnswersService,
                                       sessionRepository: SessionRepository,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: AddGuidanceView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(None) andThen requireData).async { implicit request =>
    for {
      platformOperator <- platformOperatorConnector.viewPlatformOperator(operatorId)
      userAnswers <- Future.fromTry(userAnswersService.fromPlatformOperator(request.userId, platformOperator))
      _ <- sessionRepository.set(userAnswers)
    } yield Ok(view(operatorId))
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    Redirect(AddGuidancePage.nextPage(NormalMode, operatorId, request.userAnswers))
  }
}
