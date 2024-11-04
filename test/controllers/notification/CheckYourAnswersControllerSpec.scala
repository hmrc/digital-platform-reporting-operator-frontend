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
import connectors.PlatformOperatorConnector
import connectors.PlatformOperatorConnector.UpdatePlatformOperatorFailure
import controllers.{routes => baseRoutes}
import models.operator.{AddressDetails, ContactDetails, NotificationType}
import models.operator.requests.{Notification, UpdatePlatformOperatorRequest}
import models.operator.responses.{NotificationDetails, PlatformOperator}
import models.{Country, NormalMode, UkAddress, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.notification.{NotificationTypePage, ReportingPeriodPage}
import pages.update._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.govuk.SummaryListFluency
import views.html.notification.CheckYourAnswersView

import java.time.Instant
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]
  private val mockAuditService = mock[AuditService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository, mockAuditService)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

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

      "must submit an Update Operator request, refresh this platform operator, delete notification answers and redirect to the next page" in {

        val answers =
          emptyUserAnswers
            .copy(operatorId = Some("operatorId"))
            .set(BusinessNamePage, "business").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(PrimaryContactEmailPage, "email").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value
            .set(NotificationTypePage, models.NotificationType.Epo).success.value
            .set(ReportingPeriodPage, 2024).success.value

        val expectedRequest = UpdatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorId = "operatorId",
          operatorName = "business",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(Country.ukCountries.head.code)),
          notification = Some(Notification(NotificationType.Epo, None, None, 2024))
        )

        val getPlatformOperatorResponse = PlatformOperator(
          operatorId = "operatorId",
          operatorName = "business",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(Country.ukCountries.head.code)),
          notifications = Seq(NotificationDetails(NotificationType.Epo, None, None, 2024, Instant.now))
        )

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.successful(Done)
        when(mockConnector.viewPlatformOperator(eqTo("operatorId"))(any())) thenReturn Future.successful(getPlatformOperatorResponse)
        when(mockRepository.set(any())) thenReturn Future.successful(true)
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SessionRepository].toInstance(mockRepository),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER

          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

          redirectLocation(result).value mustEqual pages.notification.CheckYourAnswersPage.nextPage(NormalMode, operatorId, answers).url
          verify(mockConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockConnector, times(1)).viewPlatformOperator(eqTo("operatorId"))(any())
          verify(mockRepository, times(1)).set(answersCaptor.capture())
          verify(mockAuditService, times(1)).sendAudit(any())(any(),any(),any())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.operatorId.value mustEqual "operatorId"
          savedAnswers.get(NotificationTypePage) must not be defined
          savedAnswers.get(ReportingPeriodPage) must not be defined

          savedAnswers.get(BusinessNamePage) mustBe defined
          savedAnswers.get(HasTradingNamePage) mustBe defined
          savedAnswers.get(HasTaxIdentifierPage) mustBe defined
          savedAnswers.get(RegisteredInUkPage) mustBe defined
          savedAnswers.get(UkAddressPage) mustBe defined
          savedAnswers.get(PrimaryContactNamePage) mustBe defined
          savedAnswers.get(PrimaryContactEmailPage) mustBe defined
          savedAnswers.get(CanPhonePrimaryContactPage) mustBe defined
          savedAnswers.get(HasSecondaryContactPage) mustBe defined
        }
      }

      "must return a failed future when creating the operator fails" in {

        val answers =
          emptyUserAnswers
            .copy(operatorId = Some("operatorId"))
            .set(BusinessNamePage, "business").success.value
            .set(HasTradingNamePage, false).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(PrimaryContactEmailPage, "email").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value
            .set(NotificationTypePage, models.NotificationType.Epo).success.value
            .set(ReportingPeriodPage, 2024).success.value

        val expectedRequest = UpdatePlatformOperatorRequest(
          subscriptionId = "dprsId",
          operatorId = "operatorId",
          operatorName = "business",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, Some("town"), None, Some("AA1 1AA"), Some(Country.ukCountries.head.code)),
          notification = Some(Notification(NotificationType.Epo, None, None, 2024))
        )

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.failed(UpdatePlatformOperatorFailure(422))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SessionRepository].toInstance(mockRepository),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockRepository, never()).set(any())
          verify(mockAuditService, times(1)).sendAudit(any())(any(),any(),any())
        }
      }

      "must return a failed future when a payload cannot be built" in {

        val answers = emptyUserAnswers.set(BusinessNamePage, "business").success.value

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SessionRepository].toInstance(mockRepository),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, never()).createPlatformOperator(any())(any())
          verify(mockRepository, never()).set(any())
          verify(mockAuditService, never()).sendAudit(any())(any(),any(),any())
        }
      }
    }
  }
}
