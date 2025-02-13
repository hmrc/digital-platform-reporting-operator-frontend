/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import controllers.AnswerExtractor
import com.google.inject.Inject
import controllers.{routes => baseRoutes}
import config.FrontendAppConfig
import connectors.AddressLookupConnector
import connectors.httpParsers.AddressLookupInitializationHttpParser.AddressLookupOnRamp
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import models.{CheckMode, Mode, NormalMode}
import pages.add.{BusinessNamePage, UkAddressPage}
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupRoutingController @Inject()(appConfig: FrontendAppConfig,
                                               sessionRepository: SessionRepository,
                                               addressLookupConnector: AddressLookupConnector,
                                               identify: IdentifierAction,
                                               cc: MessagesControllerComponents,
                                               getData: DataRetrievalActionProvider,
                                               requireData: DataRequiredAction
                                              )(implicit ec: ExecutionContext) extends FrontendController(cc) with AnswerExtractor  {

  def addressLookupInitialize(mode: Mode): Action[AnyContent] = (identify andThen getData(None) andThen requireData).async {
    implicit request =>

      getAnswerAsync(BusinessNamePage) { businessName =>
        val continueUrl = mode match {
          case NormalMode => appConfig.addressLookupContinueUrlNormalMode
          case CheckMode => appConfig.addressLookupContinueUrlCheckMode
        }

        val language: Lang = cc.messagesApi.preferred(request).lang

        addressLookupConnector.initialise(continueUrl = continueUrl,
          accessibilityFooterUrl = "", businessName)(hc: HeaderCarrier, language).flatMap {
          case Right(AddressLookupOnRamp(url)) => Future.successful(Redirect(url))
          case Left(_) => Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()))
        }
      }
  }


  def addressLookupCallback(addressId: Option[String], mode: Mode): Action[AnyContent] = (identify andThen getData(None) andThen requireData).async {
    implicit request =>

      addressId match {
        case Some(id) =>
          addressLookupConnector.getAddress(id).flatMap {
            case Right(address) =>
              val ukAddress = address.toUkAddress
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(UkAddressPage, ukAddress))
                _ <- sessionRepository.set(updatedAnswers)
              } yield {
                if (address.postcode.isEmpty || address.lines.length == 1)
                  {Redirect(controllers.add.routes.UkAddressController.onSubmit(NormalMode))}
                else
                  {Redirect(UkAddressPage.nextPage(mode, updatedAnswers))}
              }
            case _ => Future.successful(Ok(s"No Address returned on getAddress for $id"))
          }
        case _ => Future.successful(Ok("No Id returned on callback from address lookup"))
      }

  }
}
