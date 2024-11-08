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

import base.SpecBase
import builders.UserAnswersBuilder.anEmptyUserAnswer
import connectors.EmailConnector
import models.email.requests.AddedPlatformOperatorRequest
import models.subscription.{Individual, IndividualContact, SubscriptionInfo}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EmailServiceSpec extends SpecBase
  with MockitoSugar
  with FutureAwaits
  with DefaultAwaitTimeout
  with BeforeAndAfterEach {

  private val mockEmailConnector = mock[EmailConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailConnector)
    super.beforeEach()
  }

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val answers = anEmptyUserAnswer.copy(operatorId = Some("some-operator-id"))
    .set(PrimaryContactEmailPage, "email@example.com").success.value
    .set(PrimaryContactNamePage, "some-name").success.value
    .set(BusinessNamePage, "some-business-name").success.value
  private val subscriptionInfo: SubscriptionInfo = SubscriptionInfo(
    "id", gbUser = true, Some("tradingName"), IndividualContact(Individual("first", "last"), "email@example.com", None), None)
  private val requestBuildSuccess = AddedPlatformOperatorRequest.build(answers, subscriptionInfo, "some-operator-id")
  private val requestBuildFailure = AddedPlatformOperatorRequest.build(answers.remove(BusinessNamePage).success.value, subscriptionInfo, "some-operator-id")
  private val request = AddedPlatformOperatorRequest(List("email@example.com"), "dprs_added_platform_operator"
  , Map("userPrimaryContactName" -> "first last", "poBusinessName" -> "some-business-name", "poId" -> "some-operator-id")
  )
  private val underTest = new EmailService(mockEmailConnector)

  ".sendEmail" - {
    "send successful request build to email connector" in {
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(true))
      await(underTest.sendEmail(requestBuildSuccess)) mustBe true
      verify(mockEmailConnector, times(1)).send(eqTo(request))(any())
    }
    "errors from request build" in {
      await(underTest.sendEmail(requestBuildFailure)) mustBe false
      verify(mockEmailConnector, never()).send(any())(any())
    }
  }
}