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

import base.SpecBase
import builders.CreatePlatformOperatorRequestBuilder.aCreatePlatformOperatorRequest
import builders.PlatformOperatorSummaryViewModelBuilder.aPlatformOperatorSummaryViewModel
import connectors.PlatformOperatorConnector
import connectors.PlatformOperatorConnector.CreatePlatformOperatorFailure
import controllers.{routes => baseRoutes}
import models.UkTaxIdentifiers.Utr
import models.operator.responses.PlatformOperatorCreatedResponse
import models.operator.{AddressDetails, TinDetails, TinType}
import models.{Country, NormalMode, UkAddress, UkTaxIdentifiers, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.add._
import pages.update.{UkTaxIdentifiersPage, UtrPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{PlatformOperatorAddedQuery, SentAddedPlatformOperatorEmailQuery}
import repositories.SessionRepository
import services.{AuditService, EmailService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.checkAnswers.add.{BusinessNameSummary, HasSecondaryContactSummary, PrimaryContactNameSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.add.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]
  private val mockAuditService = mock[AuditService]
  private val mockEmailService = mock[EmailService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository, mockAuditService, mockEmailService)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" - {

      "when there is a second contact" in {

        val answers =
          emptyUserAnswers
            .set(BusinessNamePage, "business").success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(HasSecondaryContactPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val businessNameRow = BusinessNameSummary.row(answers)(messages(application)).value
          val primaryContactNameRow = PrimaryContactNameSummary.row(answers)(messages(application)).value
          val hasSecondaryContactRow = HasSecondaryContactSummary.row(answers)(messages(application)).value

          val platformOperatorList = SummaryListViewModel(Seq(businessNameRow))
          val primaryContactList = SummaryListViewModel(Seq(primaryContactNameRow))
          val secondaryContactList = SummaryListViewModel(Seq(hasSecondaryContactRow))

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(platformOperatorList, primaryContactList, Some(secondaryContactList))(request, messages(application)).toString
        }
      }

      "when there is no second contact" in {

        val answers =
          emptyUserAnswers
            .set(BusinessNamePage, "business").success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(HasSecondaryContactPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val businessNameRow = BusinessNameSummary.row(answers)(messages(application)).value
          val primaryContactNameRow = PrimaryContactNameSummary.row(answers)(messages(application)).value
          val hasSecondaryContactRow = HasSecondaryContactSummary.row(answers)(messages(application)).value

          val platformOperatorList = SummaryListViewModel(Seq(businessNameRow))
          val primaryContactList = SummaryListViewModel(Seq(primaryContactNameRow, hasSecondaryContactRow))

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(platformOperatorList, primaryContactList, None)(request, messages(application)).toString
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "for a POST" - {
      val anyBoolean = true
      val answers = UserAnswers(userAnswersId, Some(operatorId))
        .set(BusinessNamePage, "default-operator-name").success.value
        .set(HasTradingNamePage, false).success.value
        .set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr)).success.value
        .set(UtrPage, "default-tin").success.value
        .set(RegisteredInUkPage, true).success.value
        .set(UkAddressPage, UkAddress("default-line-1", None, "default-town", None, "default-postcode", Country("GB", "United Kingdom"))).success.value
        .set(PrimaryContactNamePage, "default-contact-name").success.value
        .set(PrimaryContactEmailPage, "default.contact@example.com").success.value
        .set(CanPhonePrimaryContactPage, false).success.value
        .set(HasSecondaryContactPage, false).success.value

      val response = PlatformOperatorCreatedResponse(operatorId)

      val expectedRequest = aCreatePlatformOperatorRequest.copy(
        tinDetails = Seq(TinDetails(
          tin = "default-tin",
          tinType = TinType.Utr,
          issuedBy = "GB"
        )),
        addressDetails = AddressDetails(
          line1 = "default-line-1",
          line2 = None,
          line3 = Some("default-town"),
          line4 = None,
          postCode = Some("default-postcode"),
          countryCode = Some("GB")
        )
      )

      "must submit a Create Operator request, clear other data from user answers and save the operator details, and redirect to the next page" in {
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

        when(mockConnector.createPlatformOperator(any())(any())) thenReturn Future.successful(response)
        when(mockRepository.set(any())) thenReturn Future.successful(true)
        when(mockEmailService.sendAddPlatformOperatorEmails(any())(any())).thenReturn(Future.successful(anyBoolean))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual CheckYourAnswersPage.nextPage(NormalMode, answers).url
          verify(mockConnector, times(1)).createPlatformOperator(eqTo(expectedRequest))(any())
          verify(mockRepository, times(1)).set(answersCaptor.capture())
          verify(mockEmailService, times(1)).sendAddPlatformOperatorEmails(eqTo(answers))(any())
          verify(mockAuditService, times(1)).sendAudit(any())(any(), any(), any())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.get(PlatformOperatorAddedQuery).value mustEqual aPlatformOperatorSummaryViewModel
          savedAnswers.get(SentAddedPlatformOperatorEmailQuery).value mustEqual anyBoolean
        }
      }

      "must return a failed future when creating the operator fails" in {
        when(mockConnector.createPlatformOperator(any())(any())) thenReturn Future.failed(CreatePlatformOperatorFailure(422))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad().url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, times(1)).createPlatformOperator(eqTo(expectedRequest))(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailService, never()).sendAddPlatformOperatorEmails(any())(any())
          verify(mockAuditService, times(1)).sendAudit(any())(any(), any(), any())
        }
      }

      "must return a failed future when a payload cannot be built" in {
        val answers = emptyUserAnswers.set(BusinessNamePage, "business").success.value
        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad().url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, never()).createPlatformOperator(any())(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailService, never()).sendAddPlatformOperatorEmails(any())(any())
          verify(mockAuditService, never()).sendAudit(any())(any(), any(), any())
        }
      }
    }
  }
}
