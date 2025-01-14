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
import connectors.SubscriptionConnector
import models.operator.NotificationType
import models.operator.responses.NotificationDetails
import models.subscription.{Individual, IndividualContact, SubscriptionInfo}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.add.PrimaryContactEmailPage
import pages.update.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{NotificationDetailsQuery, SentAddedReportingNotificationEmailQuery}
import repositories.SessionRepository
import viewmodels.checkAnswers.notification._
import viewmodels.govuk.all.SummaryListViewModel
import views.html.notification.NotificationAddedView

import java.time.Instant
import scala.concurrent.Future

class NotificationAddedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[SubscriptionConnector]
  private val mockRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository)
    super.beforeEach()
  }

  "NotificationAdded Controller" - {
    "must return OK and the correct view for a GET" - {
      val instant = Instant.parse("2024-12-31T00:00:00Z")
      val notification1 = NotificationDetails(NotificationType.Epo, None, None, 2024, instant)
      val notification2 = NotificationDetails(NotificationType.Rpo, None, Some(true), 2025, instant.plusSeconds(1))

      "for different emails" in {
        val contact = IndividualContact(Individual("first", "last"), "individualEmail", Some("phone"))
        val subscriptionInfo = SubscriptionInfo("id", gbUser = true, None, contact, None)

        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(subscriptionInfo)
        when(mockRepository.set(any())) thenReturn Future.successful(true)

        val answers = emptyUserAnswers
          .set(BusinessNamePage, "name").success.value
          .set(PrimaryContactEmailPage, "poEmail").success.value
          .set(NotificationDetailsQuery, Seq(notification1, notification2)).success.value
          .set(SentAddedReportingNotificationEmailQuery, true).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[SubscriptionConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository)
        ).build()

        running(application) {
          val request = FakeRequest(GET, routes.NotificationAddedController.onPageLoad(operatorId).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[NotificationAddedView]
          val expectedList = SummaryListViewModel(
            rows = Seq(
              OperatorNameSummary.summaryRow(answers)(messages(application)),
              OperatorIdSummary.summaryRow(answers)(messages(application)),
              NotificationTypeSummary.summaryRow(answers)(messages(application)),
              ReportingPeriodSummary.summaryRow(answers)(messages(application)),
              DueDiligenceSummary.summaryRow(answers)(messages(application)),
            ).flatten
          )
          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(operatorId, "name", expectedList, "individualEmail", "poEmail", emailSent = true)(request, messages(application)).toString
          contentAsString(result) must include(messages(application)("notificationAdded.p1.2.two.emails", "individualEmail", "poEmail"))
        }
      }

      "for same emails" in {
        val contact = IndividualContact(Individual("first", "last"), "email", Some("phone"))
        val subscriptionInfo = SubscriptionInfo("id", gbUser = true, None, contact, None)

        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(subscriptionInfo)
        when(mockRepository.set(any())) thenReturn Future.successful(true)

        val answers = emptyUserAnswers
          .set(BusinessNamePage, "name").success.value
          .set(PrimaryContactEmailPage, "email").success.value
          .set(NotificationDetailsQuery, Seq(notification1, notification2)).success.value
          .set(SentAddedReportingNotificationEmailQuery, true).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[SubscriptionConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository)
        ).build()

        running(application) {
          val request = FakeRequest(GET, routes.NotificationAddedController.onPageLoad(operatorId).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[NotificationAddedView]
          val expectedList = SummaryListViewModel(
            rows = Seq(
              OperatorNameSummary.summaryRow(answers)(messages(application)),
              OperatorIdSummary.summaryRow(answers)(messages(application)),
              NotificationTypeSummary.summaryRow(answers)(messages(application)),
              ReportingPeriodSummary.summaryRow(answers)(messages(application)),
              DueDiligenceSummary.summaryRow(answers)(messages(application)),
            ).flatten
          )
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, "name", expectedList, "email", "email", emailSent = true)(request, messages(application)).toString
          contentAsString(result) must include(messages(application)("notificationAdded.p1.2.one.email", "email"))
        }
      }

      "when emailSent is false" in {
        val contact = IndividualContact(Individual("first", "last"), "email", Some("phone"))
        val subscriptionInfo = SubscriptionInfo("id", gbUser = true, None, contact, None)

        when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(subscriptionInfo)
        when(mockRepository.set(any())) thenReturn Future.successful(true)

        val answers = emptyUserAnswers
          .set(BusinessNamePage, "name").success.value
          .set(PrimaryContactEmailPage, "email").success.value
          .set(NotificationDetailsQuery, Seq(notification1, notification2)).success.value
          .set(SentAddedReportingNotificationEmailQuery, false).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[SubscriptionConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository)
        ).build()

        running(application) {
          val request = FakeRequest(GET, routes.NotificationAddedController.onPageLoad(operatorId).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[NotificationAddedView]
          val expectedList = SummaryListViewModel(
            rows = Seq(
              OperatorNameSummary.summaryRow(answers)(messages(application)),
              OperatorIdSummary.summaryRow(answers)(messages(application)),
              NotificationTypeSummary.summaryRow(answers)(messages(application)),
              ReportingPeriodSummary.summaryRow(answers)(messages(application)),
              DueDiligenceSummary.summaryRow(answers)(messages(application)),
            ).flatten
          )
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, "name", expectedList, "email", "email", emailSent = false)(request, messages(application)).toString
          contentAsString(result) must include(messages(application)("notificationAdded.emailNotSent.warning"))
        }
      }
    }
  }
}
