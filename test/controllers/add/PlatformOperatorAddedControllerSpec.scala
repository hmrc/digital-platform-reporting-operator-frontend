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

package controllers.add

import base.SpecBase
import connectors.SubscriptionConnector
import forms.PlatformOperatorAddedFormProvider
import models.subscription.{Individual, Organisation, IndividualContact, OrganisationContact, SubscriptionInfo}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.PlatformOperatorAddedQuery
import repositories.SessionRepository
import viewmodels.PlatformOperatorSummaryViewModel
import views.html.add.PlatformOperatorAddedView
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind

import scala.concurrent.Future


class PlatformOperatorAddedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[SubscriptionConnector]
  private val mockRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository)
    super.beforeEach()
  }

  "PlatformOperatorAdded Controller" - {

    "must return OK and the correct view for an Individual GET" in {
      val contact = IndividualContact(Individual("first", "last"), "individualEmail", Some("phone"))
      val subscriptionInfo = SubscriptionInfo("id", gbUser = true, None, contact, None)

      when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(subscriptionInfo)
      when(mockRepository.set(any())) thenReturn Future.successful(true)

      val viewModel = PlatformOperatorSummaryViewModel("id", "name", "email")
      val baseAnswers = emptyUserAnswers.set(PlatformOperatorAddedQuery, viewModel).success.value
      val form = new PlatformOperatorAddedFormProvider()("name")

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SubscriptionConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository)
          )
          .build

      running(application) {
        val request = FakeRequest(GET, routes.PlatformOperatorAddedController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PlatformOperatorAddedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel, "individualEmail")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for an Organisation GET" in {
      val contact = OrganisationContact(Organisation("name"), "organisationEmail", Some("phone"))
      val subscriptionInfo = SubscriptionInfo("id", gbUser = true, None, contact, None)

      when(mockConnector.getSubscriptionInfo(any())) thenReturn Future.successful(subscriptionInfo)
      when(mockRepository.set(any())) thenReturn Future.successful(true)

      val viewModel = PlatformOperatorSummaryViewModel("id", "name", "email")
      val baseAnswers = emptyUserAnswers.set(PlatformOperatorAddedQuery, viewModel).success.value
      val form = new PlatformOperatorAddedFormProvider()("name")

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SubscriptionConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository)
          )
          .build

      running(application) {
        val request = FakeRequest(GET, routes.PlatformOperatorAddedController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PlatformOperatorAddedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel, "organisationEmail")(request, messages(application)).toString
      }
    }
  }
}
