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

import base.SpecBase
import builders.PlatformOperatorBuilder.aPlatformOperator
import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import builders.UpdatePlatformOperatorRequestBuilder.aUpdatePlatformOperatorRequest
import connectors.PlatformOperatorConnector.UpdatePlatformOperatorFailure
import connectors.SubscriptionConnector.GetSubscriptionInfoFailure
import connectors.{PlatformOperatorConnector, SubscriptionConnector}
import controllers.{routes => baseRoutes}
import models.audit.{AuditModel, ChangePlatformOperatorAuditEventModel}
import models.operator.AddressDetails
import models.{CountriesList, Country, DefaultCountriesList, UkAddress}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.update._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.OriginalPlatformOperatorQuery
import repositories.SessionRepository
import services.{AuditService, EmailService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.checkAnswers.update.{BusinessNameSummary, HasSecondaryContactSummary, PrimaryContactNameSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.update.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val mockRepository = mock[SessionRepository]
  private val mockAuditService = mock[AuditService]
  private val mockEmailService = mock[EmailService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository, mockAuditService, mockEmailService, mockSubscriptionConnector)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

    val operatorId = "default-operator-id"

    "must return OK and the correct view for a GET" - {

      "when there is a second contact" in {

        val answers =
          emptyUserAnswers
            .set(BusinessNamePage, "business").success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(HasSecondaryContactPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val businessNameRow = BusinessNameSummary.row(operatorId, answers)(messages(application)).value
          val primaryContactNameRow = PrimaryContactNameSummary.row(operatorId, answers)(messages(application)).value
          val hasSecondaryContactRow = HasSecondaryContactSummary.row(operatorId, answers)(messages(application)).value

          val platformOperatorList = SummaryListViewModel(Seq(businessNameRow))
          val primaryContactList = SummaryListViewModel(Seq(primaryContactNameRow))
          val secondaryContactList = SummaryListViewModel(Seq(hasSecondaryContactRow))

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, platformOperatorList, primaryContactList, Some(secondaryContactList))(request, messages(application)).toString
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
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val businessNameRow = BusinessNameSummary.row(operatorId, answers)(messages(application)).value
          val primaryContactNameRow = PrimaryContactNameSummary.row(operatorId, answers)(messages(application)).value
          val hasSecondaryContactRow = HasSecondaryContactSummary.row(operatorId, answers)(messages(application)).value

          val platformOperatorList = SummaryListViewModel(Seq(businessNameRow))
          val primaryContactList = SummaryListViewModel(Seq(primaryContactNameRow, hasSecondaryContactRow))

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, platformOperatorList, primaryContactList, None)(request, messages(application)).toString
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "for a POST" - {

      val platformOperator = aPlatformOperator

      val answers = emptyUserAnswers.copy(operatorId = Some("operatorId"))
        .set(BusinessNamePage, "default-operator-name").success.value
        .set(HasTradingNamePage, false).success.value
        .set(TaxResidentInUkPage, true).success.value
        .set(HasTaxIdentifierPage, false).success.value
        .set(RegisteredInUkPage, true).success.value
        .set(UkAddressPage, UkAddress("default-line-1", None, "default-town", None, "default-postcode", Country("GB", "United Kingdom"))).success.value
        .set(PrimaryContactNamePage, "default-contact-name").success.value
        .set(PrimaryContactEmailPage, "default.contact@example.com").success.value
        .set(CanPhonePrimaryContactPage, false).success.value
        .set(HasSecondaryContactPage, false).success.value
        .set(OriginalPlatformOperatorQuery, platformOperator).success.value

      val expectedRequest = aUpdatePlatformOperatorRequest.copy(subscriptionId = "dprsId", tinDetails = Seq.empty,
        addressDetails = AddressDetails(
          line1 = "default-line-1",
          line2 = None,
          line3 = Some("default-town"),
          line4 = None,
          postCode = Some("default-postcode"),
          countryCode = Some("GB")
        ),
        notification = None
      )

      val subscriptionInfo = aSubscriptionInfo
      val auditType: String = "ChangePlatformOperatorDetails"
      val countriesList = new DefaultCountriesList
      val expectedAuditEvent = ChangePlatformOperatorAuditEventModel(platformOperator, expectedRequest, countriesList)

      "must submit an Update Operator request and redirect to the next page" in {

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.successful(Done)
        when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(subscriptionInfo))
        when(mockEmailService.sendUpdatedPlatformOperatorEmails(any(), any())(any())).thenReturn(Future.successful(Done))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockConnector),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService),
          bind[CountriesList].toInstance(countriesList)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual CheckYourAnswersPage.nextPage(operatorId, answers).url
          verify(mockConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockAuditService, times(1)).sendAudit(
            eqTo(AuditModel[ChangePlatformOperatorAuditEventModel](auditType, expectedAuditEvent)))(any(), any(), any())
          verify(mockSubscriptionConnector, times(1)).getSubscriptionInfo(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailService, times(1)).sendUpdatedPlatformOperatorEmails(eqTo(answers), eqTo(subscriptionInfo))(any())

        }
      }

      "must return a failed future when updatePlatformOperator fails" in {

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.failed(UpdatePlatformOperatorFailure(422))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockConnector),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockAuditService, never()).sendAudit(any())(any(), any(), any())
          verify(mockSubscriptionConnector, never()).getSubscriptionInfo(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailService, never()).sendUpdatedPlatformOperatorEmails(any(), any())(any())
        }
      }

      "must return a failed future when getSubscriptionInfo fails" in {

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.successful(Done)
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.failed(GetSubscriptionInfoFailure(422)))
        when(mockEmailService.sendUpdatedPlatformOperatorEmails(any(), any())(any())).thenReturn(Future.successful(Done))

        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockConnector),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService),
          bind[CountriesList].toInstance(countriesList)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockAuditService, times(1)).sendAudit(
            eqTo(AuditModel[ChangePlatformOperatorAuditEventModel](auditType, expectedAuditEvent)))(any(), any(), any())
          verify(mockSubscriptionConnector, times(1)).getSubscriptionInfo(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailService, never()).sendUpdatedPlatformOperatorEmails(any(), any())(any())
        }
      }

      "must return a failed future when a payload cannot be built" in {
        val answers = emptyUserAnswers.set(BusinessNamePage, "business").success.value
        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockConnector),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[EmailService].toInstance(mockEmailService),
          bind[SessionRepository].toInstance(mockRepository)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, never()).createPlatformOperator(any())(any())
          verify(mockAuditService, never()).sendAudit(any())(any(), any(), any())
          verify(mockSubscriptionConnector, never()).getSubscriptionInfo(any())
          verify(mockEmailService, never()).sendUpdatedPlatformOperatorEmails(any(), any())(any())
          verify(mockRepository, never()).set(any())
        }
      }
    }
  }
}
