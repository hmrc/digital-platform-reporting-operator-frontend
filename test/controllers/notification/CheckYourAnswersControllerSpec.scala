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

import base.SpecBase
import builders.PlatformOperatorBuilder.aPlatformOperator
import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import builders.UpdatePlatformOperatorRequestBuilder.aUpdatePlatformOperatorRequest
import connectors.PlatformOperatorConnector.{UpdatePlatformOperatorFailure, ViewPlatformOperatorFailure}
import connectors.SubscriptionConnector.GetSubscriptionInfoFailure
import connectors.{PlatformOperatorConnector, SubscriptionConnector}
import controllers.{routes => baseRoutes}
import models.UkTaxIdentifiers.Utr
import models.operator.NotificationType.Rpo
import models.operator.requests.Notification
import models.operator.{AddressDetails, TinDetails, TinType}
import models.{Country, DueDiligence, NormalMode, UkAddress, UkTaxIdentifiers, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.notification.{DueDiligencePage, NotificationTypePage, ReportingPeriodPage}
import pages.update._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{AuditService, EmailService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.govuk.SummaryListFluency
import views.html.notification.CheckYourAnswersView

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

      val answers =
        emptyUserAnswers
          .set(BusinessNamePage, "business").success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val list = SummaryListViewModel(Nil)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(list, operatorId)(request, messages(application)).toString
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

      val answers = emptyUserAnswers.copy(operatorId = Some(operatorId))
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
        .set(NotificationTypePage, models.NotificationType.Rpo).success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(DueDiligencePage, Set[DueDiligence](DueDiligence.Extended, DueDiligence.ActiveSeller)).success.value

      val expectedRequest = aUpdatePlatformOperatorRequest.copy(subscriptionId = "dprsId", tinDetails = Seq(TinDetails(
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
        ),
        notification = Some(Notification(Rpo, Some(true), Some(true), 2024))
      )

      val getPlatformOperatorResponse = aPlatformOperator.copy(tinDetails = Seq(TinDetails(
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

      val subscriptionInfo = aSubscriptionInfo

      "must submit an Update Operator request, refresh this platform operator, delete notification answers and redirect to the next page" in {

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.successful(Done)
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockConnector.viewPlatformOperator(eqTo(operatorId))(any())) thenReturn Future.successful(getPlatformOperatorResponse)
        when(mockRepository.set(any())) thenReturn Future.successful(true)
        when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(subscriptionInfo))
        when(mockEmailService.sendAddReportingNotificationEmails(any(), any(), any())(any())).thenReturn(Future.successful(Done))

        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockConnector),
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER

          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

          redirectLocation(result).value mustEqual pages.notification.CheckYourAnswersPage.nextPage(NormalMode, operatorId, answers).url
          verify(mockConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockAuditService, times(1)).sendAudit(any())(any(), any(), any())
          verify(mockConnector, times(1)).viewPlatformOperator(eqTo(operatorId))(any())
          verify(mockSubscriptionConnector, times(1)).getSubscriptionInfo(any())
          verify(mockEmailService, times(1)).sendAddReportingNotificationEmails(eqTo(answers), eqTo(subscriptionInfo), eqTo(expectedRequest))(any())
          verify(mockRepository, times(1)).set(answersCaptor.capture())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.operatorId.value mustEqual operatorId
          savedAnswers.get(NotificationTypePage) must not be defined
          savedAnswers.get(ReportingPeriodPage) must not be defined

          savedAnswers.get(BusinessNamePage) mustBe defined
          savedAnswers.get(HasTradingNamePage) mustBe defined
          savedAnswers.get(RegisteredInUkPage) mustBe defined
          savedAnswers.get(UkAddressPage) mustBe defined
          savedAnswers.get(PrimaryContactNamePage) mustBe defined
          savedAnswers.get(PrimaryContactEmailPage) mustBe defined
          savedAnswers.get(CanPhonePrimaryContactPage) mustBe defined
          savedAnswers.get(HasSecondaryContactPage) mustBe defined
        }
      }

      "must return a failed future when updatePlatformOperator for adding a notification fails" in {

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
          verify(mockSubscriptionConnector, never()).getSubscriptionInfo(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailService, never()).sendAddReportingNotificationEmails(any(), any(), any())(any())
          verify(mockAuditService, times(1)).sendAudit(any())(any(), any(), any())
        }
      }

      "must return a failed future when viewPlatformOperator fails" in {

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.successful(Done)
        when(mockConnector.viewPlatformOperator(eqTo(operatorId))(any())) thenReturn Future.failed(ViewPlatformOperatorFailure(422))
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
          verify(mockConnector, times(1)).viewPlatformOperator(eqTo(operatorId))(any())
          verify(mockSubscriptionConnector, never()).getSubscriptionInfo(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailService, never()).sendAddReportingNotificationEmails(any(), any(), any())(any())
          verify(mockAuditService, times(1)).sendAudit(any())(any(), any(), any())
        }
      }

      "must return a failed future when getSubscriptionInfo fails" in {

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.successful(Done)
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockConnector.viewPlatformOperator(eqTo(operatorId))(any())) thenReturn Future.successful(getPlatformOperatorResponse)
        when(mockRepository.set(any())) thenReturn Future.successful(true)
        when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.failed(GetSubscriptionInfoFailure(422)))

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
          verify(mockAuditService, times(1)).sendAudit(any())(any(), any(), any())
          verify(mockConnector, times(1)).viewPlatformOperator(eqTo(operatorId))(any())
          verify(mockRepository, times(1)).set(any())
          verify(mockSubscriptionConnector, times(1)).getSubscriptionInfo(any())
          verify(mockEmailService, never()).sendAddReportingNotificationEmails(any(), any(), any())(any())

        }
      }

      "must return a failed future when a payload cannot be built" in {
        val answers = emptyUserAnswers.set(BusinessNamePage, "business").success.value
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
          verify(mockConnector, never()).createPlatformOperator(any())(any())
          verify(mockRepository, never()).set(any())
          verify(mockAuditService, never()).sendAudit(any())(any(), any(), any())
        }
      }
    }
  }
}
