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
import connectors.PlatformOperatorConnector
import models.UserAnswers
import models.operator.{AddressDetails, ContactDetails}
import models.operator.responses.PlatformOperator
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.add.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import viewmodels.PlatformOperatorViewModel
import views.html.update.{PlatformOperatorUpdatedView, PlatformOperatorView}

import scala.concurrent.Future

class PlatformOperatorControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository)
    super.beforeEach()
  }

  "Platform Operator controller" - {

    "must get platform operator details, save them, and show the correct view" in {

      val operator = PlatformOperator(
        operatorId = "operatorId",
        operatorName = "operatorName",
        tinDetails = Seq.empty,
        businessName = None,
        tradingName = None,
        primaryContactDetails = ContactDetails(None, "primaryContactName", "primaryEmail"),
        secondaryContactDetails = None,
        addressDetails = AddressDetails("line1", None, None, None, Some("postCode"), None),
        notifications = Seq.empty
      )

      when(mockConnector.viewPlatformOperator(any())(any())) thenReturn Future.successful(operator)
      when(mockRepository.set(any())) thenReturn Future.successful(true)

      val app =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository)
          )
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.PlatformOperatorController.onPageLoad("operatorId").url)

        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        val view = app.injector.instanceOf[PlatformOperatorView]
        val viewModel = PlatformOperatorViewModel("operatorId", "operatorName")

        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, messages(app)).toString
        verify(mockConnector, times(1)).viewPlatformOperator(eqTo("operatorId"))(any())
        verify(mockRepository, times(1)).set(answersCaptor.capture())

        val answers = answersCaptor.getValue
        answers.operatorId.value mustEqual "operatorId"
        answers.get(BusinessNamePage).value mustEqual "operatorName"
      }
    }
  }
}
