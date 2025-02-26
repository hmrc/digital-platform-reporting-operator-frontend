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
import builders.EmailsSentResultBuilder.anEmailsSentResult
import builders.InternationalAddressBuilder.anInternationalAddress
import builders.JerseyGuernseyIoMAddressBuilder.aJerseyGuernseyIoMAddress
import builders.PlatformOperatorBuilder.aPlatformOperator
import builders.UkAddressBuilder.aUkAddress
import builders.UpdatePlatformOperatorRequestBuilder.aUpdatePlatformOperatorRequest
import builders.UserAnswersBuilder.aUserAnswers
import connectors.PlatformOperatorConnector
import connectors.PlatformOperatorConnector.UpdatePlatformOperatorFailure
import controllers.{routes => baseRoutes}
import models.Country.Jersey
import models.RegisteredAddressCountry.{International, JerseyGuernseyIsleOfMan, Uk}
import models.UkTaxIdentifiers._
import models.audit.{AuditModel, ChangePlatformOperatorAuditEventModel}
import models.operator.{AddressDetails, TinDetails, TinType}
import models.{CountriesList, DefaultCountriesList, RegisteredAddressCountry, UkTaxIdentifiers, UserAnswers}
import org.apache.pekko.Done
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.update._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{OriginalPlatformOperatorQuery, SentUpdatedPlatformOperatorEmailQuery}
import repositories.SessionRepository
import services.{AuditService, EmailService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.checkAnswers.update._
import viewmodels.govuk.SummaryListFluency
import views.html.update.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockSessionRepository = mock[SessionRepository]
  private val mockAuditService = mock[AuditService]
  private val mockEmailService = mock[EmailService]

  override def beforeEach(): Unit = {
    reset(mockPlatformOperatorConnector, mockSessionRepository, mockAuditService, mockEmailService)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

    val operatorId = "default-operator-id"

    "must return OK and the correct view for a GET" - {

      "when there is a second contact" in {

        val answers =
          emptyUserAnswers
            .set(BusinessNamePage, "business").success.value
            .set(HasTradingNamePage, true).success.value
            .set(TradingNamePage, "tradingName").success.value
            .set(RegisteredInUkPage, International).success.value
            .set(InternationalAddressPage, anInternationalAddress).success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(PrimaryContactEmailPage, "email.com").success.value
            .set(CanPhonePrimaryContactPage, true).success.value
            .set(PrimaryContactPhoneNumberPage, "some-phone-number").success.value
            .set(HasSecondaryContactPage, true).success.value
            .set(SecondaryContactNamePage, "second Name").success.value
            .set(SecondaryContactEmailPage, "secondEmail.com").success.value
            .set(CanPhoneSecondaryContactPage, true).success.value
            .set(SecondaryContactPhoneNumberPage, "second-phone-number").success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val businessNameRow = BusinessNameSummary.row(operatorId, answers)(messages(application)).value
          val hasTradingNameRow = HasTradingNameSummary.row(operatorId, answers)(messages(application)).value
          val tradingNameRow = TradingNameSummary.row(operatorId, answers)(messages(application)).value
          val registeredInUkRow = RegisteredInUkSummary.row(operatorId, answers)(messages(application)).value
          val ukAddressRow = InternationalAddressSummary.row(operatorId, answers)(messages(application)).value
          val primaryContactNameRow = PrimaryContactNameSummary.row(operatorId, answers)(messages(application)).value
          val primaryContactEmailRow = PrimaryContactEmailSummary.row(operatorId, answers)(messages(application)).value
          val primaryContactPhoneNumberRow = PrimaryContactPhoneNumberSummary.row(operatorId, answers)(messages(application)).value
          val canPhonePrimaryContactRow = CanPhonePrimaryContactSummary.row(operatorId, answers)(messages(application)).value
          val hasSecondaryContactRow = HasSecondaryContactSummary.row(operatorId, answers)(messages(application)).value
          val secondaryContactNameRow = SecondaryContactNameSummary.row(operatorId, answers)(messages(application)).value
          val secondaryContactEmailRow = SecondaryContactEmailSummary.row(operatorId, answers)(messages(application)).value
          val canPhoneSecondaryContactRow = CanPhoneSecondaryContactSummary.row(operatorId, answers)(messages(application)).value
          val SecondaryContactPhoneNumberRow = SecondaryContactPhoneNumberSummary.row(operatorId, answers)(messages(application)).value

          val platformOperatorList = SummaryListViewModel(Seq(businessNameRow, hasTradingNameRow,
            tradingNameRow, registeredInUkRow, ukAddressRow))
          val primaryContactList = SummaryListViewModel(Seq(primaryContactNameRow, primaryContactEmailRow,
            canPhonePrimaryContactRow, primaryContactPhoneNumberRow))
          val secondaryContactList = SummaryListViewModel(Seq(hasSecondaryContactRow, secondaryContactNameRow,
            secondaryContactEmailRow, canPhoneSecondaryContactRow, SecondaryContactPhoneNumberRow))

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(operatorId, platformOperatorList, primaryContactList, Some(secondaryContactList))(request, messages(application)).toString
        }
      }

      "when there is no second contact" in {

        val answers =
          emptyUserAnswers
            .set(BusinessNamePage, "business").success.value
            .set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr, Crn, Vrn, Empref, Chrn)).success.value
            .set(UtrPage, "utr").success.value
            .set(CrnPage, "crn").success.value
            .set(VrnPage, "vrn").success.value
            .set(EmprefPage, "empref").success.value
            .set(ChrnPage, "chrn").success.value
            .set(RegisteredInUkPage, JerseyGuernseyIsleOfMan).success.value
            .set(JerseyGuernseyIoMAddressPage, aJerseyGuernseyIoMAddress).success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(HasSecondaryContactPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val businessNameRow = BusinessNameSummary.row(operatorId, answers)(messages(application)).value
          val ukTaxIdentifierRow = UkTaxIdentifiersSummary.row(operatorId, answers)(messages(application)).value
          val utrRow = UtrSummary.row(operatorId, answers)(messages(application)).value
          val crnRow = CrnSummary.row(operatorId, answers)(messages(application)).value
          val vrnRow = VrnSummary.row(operatorId, answers)(messages(application)).value
          val emprefRow = EmprefSummary.row(operatorId, answers)(messages(application)).value
          val chrnRow = ChrnSummary.row(operatorId, answers)(messages(application)).value
          val registeredInUkRow = RegisteredInUkSummary.row(operatorId, answers)(messages(application)).value
          val jerseyGuernseyIoMRow = JerseyGuernseyIoMAddressSummary.row(operatorId, answers)(messages(application)).value
          val primaryContactNameRow = PrimaryContactNameSummary.row(operatorId, answers)(messages(application)).value
          val hasSecondaryContactRow = HasSecondaryContactSummary.row(operatorId, answers)(messages(application)).value

          val platformOperatorList = SummaryListViewModel(Seq(businessNameRow, ukTaxIdentifierRow, utrRow, crnRow,
            vrnRow, emprefRow, chrnRow, registeredInUkRow, jerseyGuernseyIoMRow))
          val primaryContactList = SummaryListViewModel(Seq(primaryContactNameRow, hasSecondaryContactRow))

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, platformOperatorList, primaryContactList, None)(request, messages(application)).toString
        }
      }

      "when country is Jersey but registeredInUk was answered true" in {

        val answers =
          emptyUserAnswers
            .set(BusinessNamePage, "business").success.value
            .set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr, Crn, Vrn, Empref, Chrn)).success.value
            .set(UtrPage, "utr").success.value
            .set(CrnPage, "crn").success.value
            .set(VrnPage, "vrn").success.value
            .set(EmprefPage, "empref").success.value
            .set(ChrnPage, "chrn").success.value
            .set(RegisteredInUkPage, JerseyGuernseyIsleOfMan).success.value
            .set(UkAddressPage, aUkAddress.copy(country = Jersey)).success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(HasSecondaryContactPage, false).success.value

        val expectedAnswers = answers.set(JerseyGuernseyIoMAddressPage, aJerseyGuernseyIoMAddress).success.value
        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val businessNameRow = BusinessNameSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val ukTaxIdentifierRow = UkTaxIdentifiersSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val utrRow = UtrSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val crnRow = CrnSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val vrnRow = VrnSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val emprefRow = EmprefSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val chrnRow = ChrnSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val registeredInUkRow = RegisteredInUkSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val jerseyGuernseyIoMRow = JerseyGuernseyIoMAddressSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val primaryContactNameRow = PrimaryContactNameSummary.row(operatorId, expectedAnswers)(messages(application)).value
          val hasSecondaryContactRow = HasSecondaryContactSummary.row(operatorId, expectedAnswers)(messages(application)).value

          val platformOperatorList = SummaryListViewModel(Seq(businessNameRow, ukTaxIdentifierRow, utrRow, crnRow,
            vrnRow, emprefRow, chrnRow, registeredInUkRow, jerseyGuernseyIoMRow))
          val primaryContactList = SummaryListViewModel(Seq(primaryContactNameRow, hasSecondaryContactRow))

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, platformOperatorList, primaryContactList, None)(request, messages(application)).toString
        }
      }

      "when country is Uk" in {

        val countriesList = new CountriesList {}
        val answers =
          emptyUserAnswers
            .set(BusinessNamePage, "business").success.value
            .set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr, Crn, Vrn, Empref, Chrn)).success.value
            .set(UtrPage, "utr").success.value
            .set(CrnPage, "crn").success.value
            .set(VrnPage, "vrn").success.value
            .set(EmprefPage, "empref").success.value
            .set(ChrnPage, "chrn").success.value
            .set(RegisteredInUkPage, Uk).success.value
            .set(UkAddressPage, aUkAddress).success.value
            .set(PrimaryContactNamePage, "name").success.value
            .set(HasSecondaryContactPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val businessNameRow = BusinessNameSummary.row(operatorId, answers)(messages(application)).value
          val ukTaxIdentifierRow = UkTaxIdentifiersSummary.row(operatorId, answers)(messages(application)).value
          val utrRow = UtrSummary.row(operatorId, answers)(messages(application)).value
          val crnRow = CrnSummary.row(operatorId, answers)(messages(application)).value
          val vrnRow = VrnSummary.row(operatorId, answers)(messages(application)).value
          val emprefRow = EmprefSummary.row(operatorId, answers)(messages(application)).value
          val chrnRow = ChrnSummary.row(operatorId, answers)(messages(application)).value
          val registeredInUkRow = RegisteredInUkSummary.row(operatorId, answers)(messages(application)).value
          val ukAddressRow = UkAddressSummary.row(operatorId, answers, countriesList)(messages(application)).value
          val primaryContactNameRow = PrimaryContactNameSummary.row(operatorId, answers)(messages(application)).value
          val hasSecondaryContactRow = HasSecondaryContactSummary.row(operatorId, answers)(messages(application)).value

          val platformOperatorList = SummaryListViewModel(Seq(businessNameRow, ukTaxIdentifierRow, utrRow, crnRow,
            vrnRow, emprefRow, chrnRow, registeredInUkRow, ukAddressRow))
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
        .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
        .set(UtrPage, "utr").success.value
        .set(CrnPage, "crn").success.value
        .set(VrnPage, "vrn").success.value
        .set(EmprefPage, "empref").success.value
        .set(ChrnPage, "chrn").success.value
        .set(RegisteredInUkPage, RegisteredAddressCountry.values.head).success.value
        .set(UkAddressPage, aUkAddress).success.value
        .set(PrimaryContactNamePage, "default-contact-name").success.value
        .set(PrimaryContactEmailPage, "default.contact@example.com").success.value
        .set(CanPhonePrimaryContactPage, false).success.value
        .set(HasSecondaryContactPage, false).success.value
        .set(OriginalPlatformOperatorQuery, platformOperator).success.value

      val expectedRequest = aUpdatePlatformOperatorRequest.copy(subscriptionId = "dprsId",
        tinDetails = Seq(
          TinDetails("crn", TinType.Crn, "GB"),
          TinDetails("empref", TinType.Empref, "GB"),
          TinDetails("vrn", TinType.Vrn, "GB"),
          TinDetails("chrn", TinType.Chrn, "GB"),
          TinDetails("utr", TinType.Utr, "GB")
        ),
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

      val auditType: String = "ChangePlatformOperatorDetails"
      val countriesList = new DefaultCountriesList
      val expectedAuditEvent = ChangePlatformOperatorAuditEventModel(platformOperator, expectedRequest, countriesList)

      "must submit an Update Operator request and redirect to the next page" in {
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

        when(mockPlatformOperatorConnector.updatePlatformOperator(any())(any())) thenReturn Future.successful(Done)
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockEmailService.sendUpdatedPlatformOperatorEmails(any())(any())).thenReturn(Future.successful(anEmailsSentResult))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService),
          bind[CountriesList].toInstance(countriesList)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual CheckYourAnswersPage.nextPage(operatorId, answers).url
          verify(mockPlatformOperatorConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockAuditService, times(1)).sendAudit(
            eqTo(AuditModel[ChangePlatformOperatorAuditEventModel](auditType, expectedAuditEvent)))(any(), any(), any())
          verify(mockSessionRepository, times(1)).set(answersCaptor.capture())
          verify(mockEmailService, times(1)).sendUpdatedPlatformOperatorEmails(eqTo(answers))(any())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.get(SentUpdatedPlatformOperatorEmailQuery).value mustEqual anEmailsSentResult
        }
      }

      "must return a failed future when updatePlatformOperator fails" in {
        when(mockPlatformOperatorConnector.updatePlatformOperator(any())(any())) thenReturn Future.failed(UpdatePlatformOperatorFailure(422))
        when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[EmailService].toInstance(mockEmailService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)

          route(app, request).value.failed.futureValue
          verify(mockPlatformOperatorConnector, times(1)).updatePlatformOperator(eqTo(expectedRequest))(any())
          verify(mockAuditService, never).sendAudit(any())(any(), any(), any())
          verify(mockSessionRepository, never).set(any())
          verify(mockEmailService, never).sendUpdatedPlatformOperatorEmails(any())(any())
        }
      }

      "must return a failed future when a payload cannot be built" in {
        val answers = emptyUserAnswers.set(BusinessNamePage, "business").success.value
        val app = applicationBuilder(Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[EmailService].toInstance(mockEmailService),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(operatorId).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.MissingInformationController.onPageLoad(operatorId).url

          verify(mockPlatformOperatorConnector, never).createPlatformOperator(any())(any())
          verify(mockAuditService, never).sendAudit(any())(any(), any(), any())
          verify(mockEmailService, never).sendUpdatedPlatformOperatorEmails(any())(any())
          verify(mockSessionRepository, never).set(any())
        }
      }
    }

    ".initialise(...)" - {
      "must redirect to Check Your Answers page" in {
        val answers = aUserAnswers.copy(userId = userAnswersId)
          .set(BusinessNamePage, "some-operator-name").success.value
          .set(HasTradingNamePage, false).success.value
          .set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](UkTaxIdentifiers.Utr)).success.value
          .set(UtrPage, "utr").success.value
          .set(RegisteredInUkPage, RegisteredAddressCountry.Uk).success.value
          .set(UkAddressPage, aUkAddress).success.value
          .set(PrimaryContactNamePage, "some-contact-name").success.value
          .set(PrimaryContactEmailPage, "some.contact@example.com").success.value
          .set(CanPhonePrimaryContactPage, false).success.value
          .set(HasSecondaryContactPage, false).success.value

        when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(aPlatformOperator))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val app = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        running(app) {
          val result = route(app, FakeRequest(GET, routes.CheckYourAnswersController.initialise(answers.operatorId.get).url)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(answers.operatorId.get).url
        }

        verify(mockPlatformOperatorConnector, times(1)).viewPlatformOperator(eqTo(answers.operatorId.get))(any())
      }
    }
  }
}
