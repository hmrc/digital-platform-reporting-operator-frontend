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
import controllers.{routes => baseRoutes}
import forms.SelectPlatformOperatorToViewFormProvider
import models.NormalMode
import models.operator.PlatformOperatorData
import pages.notification.SelectPlatformOperatorToViewPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.UserAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.notification.SelectPlatformOperatorToViewView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectPlatformOperatorToViewController @Inject()(override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       formProvider: SelectPlatformOperatorToViewFormProvider,
                                                       view: SelectPlatformOperatorToViewView,
                                                       connector: PlatformOperatorConnector,
                                                       userAnswersService: UserAnswersService,
                                                       sessionRepository: SessionRepository)
                                                      (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>
    connector.viewPlatformOperators.map { operatorInfo =>
      val operators = operatorInfo.platformOperators.map(PlatformOperatorData(_))
      val form = formProvider(operators.map(_.operatorId).toSet)

      Ok(view(form, operators))
    }
  }

  def onSubmit: Action[AnyContent] = identify.async { implicit request =>
    connector.viewPlatformOperators.flatMap { operatorInfo =>
      val operators = operatorInfo.platformOperators.map(PlatformOperatorData(_))
      val form = formProvider(operators.map(_.operatorId).toSet)

      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, operators))),
        operatorId =>
          operatorInfo.platformOperators.find(_.operatorId == operatorId).map { operator =>
            for {
              userAnswers <- Future.fromTry(userAnswersService.fromPlatformOperator(request.userId, operator))
              _           <- sessionRepository.set(userAnswers)
            } yield Redirect(SelectPlatformOperatorToViewPage.nextPage(NormalMode, operatorId, userAnswers))
          }.getOrElse {
            Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()))
          }
      )
    }
  }
}
