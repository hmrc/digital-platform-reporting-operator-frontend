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

package views.add

import base.ViewSpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import viewmodels.PlatformOperatorSummaryViewModel
import views.html.add.PlatformOperatorAddedView

class PlatformOperatorAddedViewSpec extends ViewSpecBase {
  val page: PlatformOperatorAddedView = inject[PlatformOperatorAddedView]

  "PlatformOperatorAddedView" should {

    "with different emails" should {

      val platformOperatorSummaryViewModel = new PlatformOperatorSummaryViewModel("operatorId", "operatorName", "poPrimaryContactEmail")
      val subscriptionEmail = "subscriptionEmail"
      val view: Document = Jsoup.parse(page(platformOperatorSummaryViewModel, subscriptionEmail)(request, messages).toString())

      "have a title" in {
        view.getElementsByTag("title").text must include("Platform operator added - Report information of sellers using your digital platform - GOV.UK GOV.UK")
      }

      "have a panel" in {
        view.getElementsByClass("govuk-panel__title").text must include("operatorName added")
        view.getElementsByClass("govuk-panel__body").text must include("The platform operator ID is operatorId")
      }

      "have the correct paragraphs and links" in {

        view.getElementsByClass("govuk-body").get(0).text must include(
          "operatorName has been successfully added as a platform operator. " +
            "We have sent a confirmation email to subscriptionEmail and poPrimaryContactEmail."
        )

        view.getElementsByClass("govuk-body").get(1).text must include(
          "Keep a record of this platform operator ID. " +
            "You need to provide it on every file you send to HMRC for this platform operator."
        )
        view.getElementsByClass("govuk-body").get(2).text must include(
          "To send submissions for this platform operator, you must"
        )
        view.getElementsByClass("govuk-body").get(2).getElementsByClass("govuk-link").text must include(
          "add a reporting notification"
        )
        view.getElementsByClass("govuk-body").get(2).getElementsByClass("govuk-link").attr("href") must include(
          appConfig.manageFrontendUrl
        )
        view.getElementsByClass("govuk-body").get(3).text must include(
          "You can also"
        )
        view.getElementsByClass("govuk-body").get(3).getElementsByClass("govuk-link").text must include(
          "add another platform operator"
        )
        view.getElementsByClass("govuk-body").get(3).getElementsByClass("govuk-link").attr("href") must include(
          controllers.add.routes.StartController.onPageLoad().url
        )
      }

      "have a sub heading" in {
        view.getElementsByTag("h2").first.text must include("What you need to do next")
      }
    }

    "with the same emails" should {

      val platformOperatorSummaryViewModel = new PlatformOperatorSummaryViewModel("operatorId", "operatorName", "subscriptionEmail")
      val subscriptionEmail = "subscriptionEmail"
      val view: Document = Jsoup.parse(page(platformOperatorSummaryViewModel, subscriptionEmail)(request, messages).toString())

      "have the correct paragraph" in {
        view.getElementsByClass("govuk-body").get(0).text must include(
          "operatorName has been successfully added as a platform operator. " +
            "We have sent a confirmation email to subscriptionEmail."
        )
      }

    }

  }
}
