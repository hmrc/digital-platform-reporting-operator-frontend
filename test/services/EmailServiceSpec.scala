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
import connectors.SubscriptionConnector.GetSubscriptionInfoFailure
import connectors.{EmailConnector, SubscriptionConnector}
import models.email.requests._
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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailServiceSpec extends AnyFreeSpec
  with Matchers
  with MockitoSugar
  with FutureAwaits
  with TryValues
  with DefaultAwaitTimeout
  with BeforeAndAfterEach {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val anyBoolean = true

  private val mockEmailConnector = mock[EmailConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailConnector, mockSubscriptionConnector)
    super.beforeEach()
  }

  private val underTest = new EmailService(mockEmailConnector, mockSubscriptionConnector)

  ".sendAddPlatformOperatorEmails(...)" - {
    "return false when getSubscriptionInfo fails" in {
      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.failed(GetSubscriptionInfoFailure(500)))

      underTest.sendAddPlatformOperatorEmails(aUserAnswers).futureValue mustBe false
    }

    "non-matching emails must send both AddedPlatformOperatorRequest AddedAsPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "some-email").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedAddedPORequest = AddedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedAddedAsPORequest = AddedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(true))

      underTest.sendAddPlatformOperatorEmails(userAnswers).futureValue mustBe true

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedPORequest))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedAsPORequest))(any())
    }

    "matching emails must send only AddedPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "default.email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedAddedPORequest = AddedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedAddedAsPORequest = AddedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(true))

      underTest.sendAddPlatformOperatorEmails(userAnswers).futureValue mustBe true

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedPORequest))(any())
      verify(mockEmailConnector, never()).send(eqTo(expectedAddedAsPORequest))(any())
    }

    "must not send emails when relevant data not available" in {
      val userAnswers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))

      underTest.sendAddPlatformOperatorEmails(userAnswers).futureValue mustBe false

      verify(mockEmailConnector, never()).send(any())(any())
    }
  }

  ".sendRemovePlatformOperatorEmails(...)" - {
    "return false when getSubscriptionInfo fails" in {
      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.failed(GetSubscriptionInfoFailure(500)))

      underTest.sendRemovePlatformOperatorEmails(aUserAnswers).futureValue mustBe false
    }

    "non-matching emails must send both RemovedPlatformOperatorRequest RemovedAsPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "some-email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedRemovedPORequest = RemovedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedRemovedAsPORequest = RemovedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendRemovePlatformOperatorEmails(userAnswers).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedRemovedPORequest))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedRemovedAsPORequest))(any())
    }

    "matching emails must send only RemovedPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "default.email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedRemovedPORequest = RemovedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedRemovedAsPORequest = RemovedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendRemovePlatformOperatorEmails(userAnswers).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedRemovedPORequest))(any())
      verify(mockEmailConnector, never()).send(eqTo(expectedRemovedAsPORequest))(any())
    }

    "must not send emails when relevant data not available" in {
      val userAnswers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendRemovePlatformOperatorEmails(userAnswers).futureValue

      verify(mockEmailConnector, never()).send(any())(any())
    }
  }

  ".sendUpdatedPlatformOperatorEmails(...)" - {
    "must return false when getSubscriptionInfo fails" in {
      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.failed(GetSubscriptionInfoFailure(500)))

      underTest.sendUpdatedPlatformOperatorEmails(aUserAnswers).futureValue mustBe false
    }

    "non-matching emails must send both UpdatedPlatformOperatorRequest UpdatedAsPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "some-email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedUpdatedPORequest = UpdatedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedUpdatedAsPORequest = UpdatedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendUpdatedPlatformOperatorEmails(userAnswers).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdatedPORequest))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdatedAsPORequest))(any())
    }

    "matching emails must send only UpdatedPlatformOperatorRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "default.email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedUpdatedPORequest = UpdatedPlatformOperatorRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedUpdatedAsPORequest = UpdatedAsPlatformOperatorRequest.build(userAnswers).toOption.get

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendUpdatedPlatformOperatorEmails(userAnswers).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdatedPORequest))(any())
      verify(mockEmailConnector, never()).send(eqTo(expectedUpdatedAsPORequest))(any())
    }

    "must not send emails when relevant data not available" in {
      val userAnswers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendUpdatedPlatformOperatorEmails(userAnswers).futureValue

      verify(mockEmailConnector, never()).send(any())(any())
    }
  }

  ".sendAddReportingNotificationEmails(...)" - {
    "must return false when getSubscriptionInfo fails" in {
      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.failed(GetSubscriptionInfoFailure(500)))

      underTest.sendAddReportingNotificationEmails(aUserAnswers, aUpdatePlatformOperatorRequest).futureValue mustBe false
    }

    "non-matching emails must send both AddedReportingNotificationRequest, AddedAsReportingNotificationRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "some-email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedAddedRNRequest = AddedReportingNotificationRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedAddedAsRNRequest = AddedAsReportingNotificationRequest.build(userAnswers, aUpdatePlatformOperatorRequest).toOption.get

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendAddReportingNotificationEmails(userAnswers, aUpdatePlatformOperatorRequest).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedRNRequest))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedAsRNRequest))(any())
    }

    "matching emails must send only AddedReportingNotificationRequest when relevant data available" in {
      val userAnswers = aUserAnswers
        .set(PrimaryContactEmailPage, "default.email@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(BusinessNamePage, "some-business-name").success.value
      val expectedAddedRNRequest = AddedReportingNotificationRequest.build(userAnswers, aSubscriptionInfo).toOption.get
      val expectedAddedAsRNRequest = AddedAsReportingNotificationRequest.build(userAnswers, aUpdatePlatformOperatorRequest).toOption.get

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendAddReportingNotificationEmails(userAnswers, aUpdatePlatformOperatorRequest).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddedRNRequest))(any())
      verify(mockEmailConnector, never()).send(eqTo(expectedAddedAsRNRequest))(any())
    }

    "must not send emails when relevant data not available" in {
      val userAnswers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      when(mockSubscriptionConnector.getSubscriptionInfo(any())).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendAddReportingNotificationEmails(userAnswers, aUpdatePlatformOperatorRequest).futureValue

      verify(mockEmailConnector, never()).send(any())(any())
    }
  }
}