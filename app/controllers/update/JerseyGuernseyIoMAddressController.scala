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

import controllers.AnswerExtractor
import controllers.actions._
import forms.JerseyGuernseyIoMAddressFormProvider
import models.{CountriesList, JerseyGuernseyIoMAddress, UkAddress}
import pages.add.UkAddressPage
import pages.update.{BusinessNamePage, JerseyGuernseyIoMAddressPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.update.JerseyGuernseyIoMAddressView

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

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData) { implicit request =>
    getAnswer(BusinessNamePage) { businessName =>
      val form = formProvider()
      val preparedForm = request.userAnswers.get(JerseyGuernseyIoMAddressPage) match {
        case None => request.userAnswers.get(UkAddressPage) match {
          case Some(value) if countriesList.crownDependantCountries.exists(_.code == value.country.code) => form.fill(convertToJerseyGuernseyIoMAddress(value))
          case _ => form
        }

        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, operatorId, businessName))
    }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(Some(operatorId)) andThen requireData).async { implicit request =>
    getAnswerAsync(BusinessNamePage) { businessName =>
      formProvider().bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, operatorId, businessName))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(JerseyGuernseyIoMAddressPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(JerseyGuernseyIoMAddressPage.nextPage(operatorId, updatedAnswers))
      )
    }
  }

  private def convertToJerseyGuernseyIoMAddress(ukAddress: UkAddress): JerseyGuernseyIoMAddress = {
    JerseyGuernseyIoMAddress(ukAddress.line1,
      ukAddress.line2,
      ukAddress.town,
      ukAddress.county,
      ukAddress.postCode,
      ukAddress.country)
  }
}
