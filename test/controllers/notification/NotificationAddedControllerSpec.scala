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
import models.operator.NotificationType
import models.operator.responses.NotificationDetails
import pages.update.BusinessNamePage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.NotificationDetailsQuery
import viewmodels.checkAnswers.notification.{DueDiligenceSummary, NotificationTypeSummary, OperatorIdSummary, OperatorNameSummary, ReportingPeriodSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.notification.NotificationAddedView
import viewmodels.govuk.summarylist._

import java.time.Instant

class NotificationAddedControllerSpec extends SpecBase {

  "NotificationAdded Controller" - {

    "must return OK and the correct view for a GET" in {

      val instant = Instant.parse("2024-12-31T00:00:00Z")

      val notification1 = NotificationDetails(NotificationType.Epo, None, None, 2024, instant)
      val notification2 = NotificationDetails(NotificationType.Rpo, None, Some(true), 2025, instant.plusSeconds(1))

      val answers =
        emptyUserAnswers
          .set(BusinessNamePage, "name").success.value
          .set(NotificationDetailsQuery, Seq(notification1, notification2)).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

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
        ).withCssClass("govuk-summary-list--long-key")


        status(result) mustEqual OK
        contentAsString(result) mustEqual view(operatorId, "name", expectedList)(request, messages(application)).toString
      }
    }
  }
}
