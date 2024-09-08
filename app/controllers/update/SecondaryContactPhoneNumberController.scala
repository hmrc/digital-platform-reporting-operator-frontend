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
import forms.update.SecondaryContactPhoneNumberFormProvider
import pages.update.{BusinessNamePage, SecondaryContactNamePage, SecondaryContactPhoneNumberPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.update.SecondaryContactPhoneNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SecondaryContactPhoneNumberController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalActionProvider,
                                        requireData: DataRequiredAction,
                                        formProvider: SecondaryContactPhoneNumberFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: SecondaryContactPhoneNumberView
                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswers(BusinessNamePage, SecondaryContactNamePage) { case (businessName, contactName) =>

      val form = formProvider(contactName)

      val preparedForm = request.userAnswers.get(SecondaryContactPhoneNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

       Ok(view(preparedForm, operatorId, businessName, contactName))
    }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async { implicit request =>
    getAnswersAsync(BusinessNamePage, SecondaryContactNamePage) { case (businessName, contactName) =>

      val form = formProvider(contactName)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, operatorId, businessName, contactName))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SecondaryContactPhoneNumberPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(SecondaryContactPhoneNumberPage.nextPage(operatorId, updatedAnswers))
      )
    }
  }
}
