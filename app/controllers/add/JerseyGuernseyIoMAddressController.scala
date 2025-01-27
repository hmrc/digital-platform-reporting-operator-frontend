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

package controllers.add

import controllers.AnswerExtractor
import controllers.actions._
import forms.JerseyGuernseyIoMAddressFormProvider
import models.{CountriesList, Mode}
import pages.add.{BusinessNamePage, JerseyGuernseyIoMAddressPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.add.JerseyGuernseyIoMAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JerseyGuernseyIoMAddressController @Inject()(override val messagesApi: MessagesApi,
                                                   sessionRepository: SessionRepository,
                                                   identify: IdentifierAction,
                                                   getData: DataRetrievalActionProvider,
                                                   requireData: DataRequiredAction,
                                                   formProvider: JerseyGuernseyIoMAddressFormProvider,
                                                   val countriesList: CountriesList,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: JerseyGuernseyIoMAddressView)
                                                  (implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData(None) andThen requireData) { implicit request =>
    getAnswer(BusinessNamePage) { businessName =>
      val preparedForm = request.userAnswers.get(JerseyGuernseyIoMAddressPage) match {
        case None => formProvider()
        case Some(value) => formProvider().fill(value)
      }

      Ok(view(preparedForm, mode, businessName))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData(None) andThen requireData).async { implicit request =>
    getAnswerAsync(BusinessNamePage) { businessName =>
      formProvider().bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, businessName))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(JerseyGuernseyIoMAddressPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(JerseyGuernseyIoMAddressPage.nextPage(mode, updatedAnswers))
      )
    }
  }
}
