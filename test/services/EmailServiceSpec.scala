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

package services

import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import builders.UpdatePlatformOperatorRequestBuilder.aUpdatePlatformOperatorRequest
import builders.UserAnswersBuilder.aUserAnswers
import connectors.EmailConnector
import models.email.requests._
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}
import pages.notification.ReportingPeriodPage
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EmailServiceSpec extends AnyFreeSpec
  with Matchers
  with MockitoSugar
  with FutureAwaits
  with TryValues
  with DefaultAwaitTimeout
  with BeforeAndAfterEach {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val mockEmailConnector = mock[EmailConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailConnector)
    super.beforeEach()
  }

  private val underTest = new EmailService(mockEmailConnector)

  ".sendAddPlatformOperatorEmails(...)" - {
    "must send AddedPlatformOperatorRequest AddedAsPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "some-email").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedAddedPORequest = AddedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedAddedAsPORequest = AddedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendAddPlatformOperatorEmails(userAnswers, aSubscriptionInfo).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedPORequest))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedAsPORequest))(any())
    }

    "must not send emails when relevant data not available" in {
      val userAnswers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendAddPlatformOperatorEmails(userAnswers, aSubscriptionInfo).futureValue

      verify(mockEmailConnector, never()).send(any())(any())
      verify(mockEmailConnector, never()).send(any())(any())
    }
  }

  ".sendRemovePlatformOperatorEmails(...)" - {
    "must send RemovedPlatformOperatorRequest RemovedAsPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "some-email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedRemovedPORequest = RemovedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedRemovedAsPORequest = RemovedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendRemovePlatformOperatorEmails(userAnswers, aSubscriptionInfo).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedRemovedPORequest))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedRemovedAsPORequest))(any())
    }

    "must not send emails when relevant data not available" in {
      val userAnswers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendRemovePlatformOperatorEmails(userAnswers, aSubscriptionInfo).futureValue

      verify(mockEmailConnector, never()).send(any())(any())
      verify(mockEmailConnector, never()).send(any())(any())
    }
  }

  ".sendUpdatedPlatformOperatorEmails(...)" - {
    "must send UpdatedPlatformOperatorRequest UpdatedAsPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "some-email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedUpdatedPORequest = UpdatedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedUpdatedAsPORequest = UpdatedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendUpdatedPlatformOperatorEmails(userAnswers, aSubscriptionInfo).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdatedPORequest))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdatedAsPORequest))(any())
    }

    "must not send emails when relevant data not available" in {
      val userAnswers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendUpdatedPlatformOperatorEmails(userAnswers, aSubscriptionInfo).futureValue

      verify(mockEmailConnector, never()).send(any())(any())
      verify(mockEmailConnector, never()).send(any())(any())
    }
  }

  ".sendAddReportingNotificationEmails(...)" - {
    "must send AddedReportingNotificationRequest, AddedAsReportingNotificationRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "some-email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedAddedRNRequest = AddedReportingNotificationRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedAddedAsRNRequest = AddedAsReportingNotificationRequest.build(userAnswers, aUpdatePlatformOperatorRequest).toOption.get

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendAddReportingNotificationEmails(userAnswers, aSubscriptionInfo, aUpdatePlatformOperatorRequest).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedRNRequest))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedAsRNRequest))(any())
    }

    "must not send emails when relevant data not available" in {
      val userAnswers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendAddReportingNotificationEmails(userAnswers, aSubscriptionInfo, aUpdatePlatformOperatorRequest).futureValue

      verify(mockEmailConnector, never()).send(any())(any())
      verify(mockEmailConnector, never()).send(any())(any())
    }
  }
}