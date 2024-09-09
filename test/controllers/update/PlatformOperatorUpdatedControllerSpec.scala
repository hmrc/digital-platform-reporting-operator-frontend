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
import pages.update.PlatformOperatorUpdatedPage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.PlatformOperatorAddedQuery
import viewmodels.PlatformOperatorAddedViewModel
import views.html.update.PlatformOperatorUpdatedView

class PlatformOperatorUpdatedControllerSpec extends SpecBase {

  "PlatformOperatorUpdated Controller" - {

    "must return OK and the correct view for a GET" in {

      val viewModel = PlatformOperatorAddedViewModel("id", "name")
      val baseAnswers = emptyUserAnswers.set(PlatformOperatorAddedQuery, viewModel).success.value

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.PlatformOperatorUpdatedController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PlatformOperatorUpdatedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(operatorId, viewModel)(request, messages(application)).toString
      }
    }

    "must redirect to the next page for a POST" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.PlatformOperatorUpdatedController.onSubmit(operatorId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual PlatformOperatorUpdatedPage.nextPage(operatorId, emptyUserAnswers).url
      }
    }
  }
}
