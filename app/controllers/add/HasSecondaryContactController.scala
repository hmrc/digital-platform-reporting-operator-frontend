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

import controllers.AnswerExtractor
import controllers.actions._
import forms.HasSecondaryContactFormProvider
import models.Mode
import pages.add.{HasSecondaryContactPage, PrimaryContactNamePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.add.HasSecondaryContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasSecondaryContactController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalActionProvider,
                                         requireData: DataRequiredAction,
                                         formProvider: HasSecondaryContactFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: HasSecondaryContactView
                                 )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData(None) andThen requireData) { implicit request =>
    getAnswer(PrimaryContactNamePage) { primaryContactName =>

      val form = formProvider()

      val preparedForm = request.userAnswers.get(HasSecondaryContactPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

       Ok(view(preparedForm, mode, primaryContactName))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData(None) andThen requireData).async { implicit request =>
    getAnswerAsync(PrimaryContactNamePage) { primaryContactName =>

      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, primaryContactName))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(HasSecondaryContactPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(HasSecondaryContactPage.nextPage(mode, updatedAnswers))
      )
    }
  }
}
