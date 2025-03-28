/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.actions._
import controllers.{routes => baseRoutes}
import models.operator.requests.UpdatePlatformOperatorRequest
import pages.update.UpdateQuestionPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.Query
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.update.MissingInformationView

import javax.inject.Inject
import scala.concurrent.Future

class MissingInformationController @Inject()(override val messagesApi: MessagesApi,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalActionProvider,
                                             requireData: DataRequiredAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: MissingInformationView) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    Ok(view(operatorId))
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async { implicit request =>
    UpdatePlatformOperatorRequest.build(request.userAnswers, request.dprsId, operatorId).fold(
      errors => Future.successful(Redirect(findRoute(operatorId, errors.head).url)),
      _ => Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad(operatorId)))
    )
  }

  private def findRoute(operatorId: String, error: Query): Call = error match {
    case page: UpdateQuestionPage[_] => page.route(operatorId)
    case _ => baseRoutes.JourneyRecoveryController.onPageLoad()
  }
}
