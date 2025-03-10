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

import controllers.AnswerExtractor
import controllers.actions._
import forms.UkTaxIdentifiersFormProvider
import pages.update.{BusinessNamePage, UkTaxIdentifiersPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.update.UkTaxIdentifiersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkTaxIdentifiersController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalActionProvider,
                                        requireData: DataRequiredAction,
                                        formProvider: UkTaxIdentifiersFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: UkTaxIdentifiersView
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswer(BusinessNamePage) { businessName =>

      val form = formProvider()

      val preparedForm = request.userAnswers.get(UkTaxIdentifiersPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

       Ok(view(preparedForm, operatorId, businessName))
    }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async { implicit request =>
    getAnswerAsync(BusinessNamePage) { businessName =>

      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, operatorId, businessName))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UkTaxIdentifiersPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(UkTaxIdentifiersPage.nextPage(operatorId, updatedAnswers))
      )
    }
  }
}
