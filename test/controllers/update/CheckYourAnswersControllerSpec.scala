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
import connectors.{EmailConnector, PlatformOperatorConnector, SubscriptionConnector}
import controllers.{routes => baseRoutes}
import models.audit.{AuditModel, ChangePlatformOperatorAuditEventModel}
import models.email.requests.{UpdatedAsPlatformOperatorRequest, UpdatedPlatformOperatorRequest}
import models.operator.requests.UpdatePlatformOperatorRequest
import models.operator.responses.{NotificationDetails, PlatformOperator}
import models.operator.{AddressDetails, ContactDetails, NotificationType}
import models.subscription.{Individual, IndividualContact, SubscriptionInfo}
import models.{Country, UkAddress}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.update._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.OriginalPlatformOperatorQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.checkAnswers.update.{BusinessNameSummary, HasSecondaryContactSummary, PrimaryContactNameSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.update.CheckYourAnswersView

import java.time.Instant
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val mockRepository = mock[SessionRepository]
  private val mockAuditService = mock[AuditService]
  private val mockEmailConnector = mock[EmailConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository, mockAuditService, mockEmailConnector, mockSubscriptionConnector)
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
          contentAsString(result) mustEqual view(operatorId,platformOperatorList, primaryContactList, Some(secondaryContactList))(request, messages(application)).toString
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

      "must submit an Update Operator request and redirect to the next page" in {

        val platformOperator = PlatformOperator(
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

        val answers =
          emptyUserAnswers
            .copy(operatorId = Some("operatorId"))
            .set(BusinessNamePage, "business").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(PrimaryContactEmailPage, "email").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value
            .set(OriginalPlatformOperatorQuery, platformOperator).success.value

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
          notification = None
        )

        val expectedSendEmailRequest = UpdatedPlatformOperatorRequest("email", "first last", "business", operatorId)
        val expectedSendAsEmailRequest = UpdatedAsPlatformOperatorRequest("email", "name", "business", operatorId)
        val subscriptionInfo = SubscriptionInfo("id", gbUser = true, Some("tradingName"), IndividualContact(Individual("first", "last"), "email", None), None)

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.successful(Done)
        when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(subscriptionInfo))
        when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(true))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
              bind[SessionRepository].toInstance(mockRepository),
              bind[EmailConnector].toInstance(mockEmailConnector),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)
          val auditType: String = "ChangePlatformOperatorDetails"
          val expectedAuditEvent = ChangePlatformOperatorAuditEventModel(platformOperator, expectedRequest)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual CheckYourAnswersPage.nextPage(operatorId, answers).url
          verify(mockConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockSubscriptionConnector, times(1)).getSubscriptionInfo(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailConnector, times(1)).send(eqTo(expectedSendEmailRequest))(any())
          verify(mockEmailConnector, times(1)).send(eqTo(expectedSendAsEmailRequest))(any())
          verify(mockAuditService, times(1)).sendAudit(
            eqTo(AuditModel[ChangePlatformOperatorAuditEventModel](auditType, expectedAuditEvent)))(any(),any(),any())
        }
      }

      "must return a failed future when updating the operator fails" in {

        val answers =
          emptyUserAnswers
            .copy(operatorId = Some("operatorId"))
            .set(BusinessNamePage, "business").success.value
            .set(HasTradingNamePage, false).success.value
            .set(TaxResidentInUkPage, true).success.value
            .set(HasTaxIdentifierPage, false).success.value
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(PrimaryContactEmailPage, "email").success.value
            .set(CanPhonePrimaryContactPage, false).success.value
            .set(HasSecondaryContactPage, false).success.value

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
          notification = None
        )

        when(mockConnector.updatePlatformOperator(any())(any())) thenReturn Future.failed(new Exception("foo"))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
              bind[SessionRepository].toInstance(mockRepository),
              bind[EmailConnector].toInstance(mockEmailConnector),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockSubscriptionConnector, never()).getSubscriptionInfo(any())
          verify(mockRepository, never()).set(any())
          verify(mockEmailConnector, never()).send(any())(any())
          verify(mockAuditService, never()).sendAudit(any())(any(),any(),any())
        }
      }

      "must return a failed future when a payload cannot be built" in {

        val answers = emptyUserAnswers.set(BusinessNamePage, "business").success.value

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[PlatformOperatorConnector].toInstance(mockConnector),
              bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
              bind[EmailConnector].toInstance(mockEmailConnector),
              bind[SessionRepository].toInstance(mockRepository)
            )
            .build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          route(app, request).value.failed.futureValue
          verify(mockConnector, never()).createPlatformOperator(any())(any())
          verify(mockSubscriptionConnector, never()).getSubscriptionInfo(any())
          verify(mockEmailConnector, never()).send(any())(any())
          verify(mockRepository, never()).set(any())
        }
      }
    }
  }
}
