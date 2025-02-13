/*
 * Copyright 2023 HM Revenue & Customs
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

package config

import com.google.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{JsObject, Json}

class AddressLookupConfig @Inject()(messagesApi: MessagesApi, appConfig: FrontendAppConfig) {

  def config(continueUrl:String, accessibilityFooterUrl: String, businessName: String)(implicit language: Lang): JsObject = {

    val v2Config = s"""{
                      |  "version": 2,
                      |  "options": {
                      |    "continueUrl": "$continueUrl",
                      |    "accessibilityFooterUrl": "$accessibilityFooterUrl",
                      |    "signOutHref": "${appConfig.signOutUrl}",
                      |    "phaseFeedbackLink": "/help/alpha",
                      |    "showPhaseBanner": true,
                      |    "alphaPhase": false,
                      |    "showBackButtons": true,
                      |    "includeHMRCBranding": false,
                      |    "disableTranslations": true,
                      |    "ukMode": true,
                      |    "selectPageConfig": {
                      |      "proposalListLimit": 50,
                      |      "showSearchLinkAgain": true
                      |    },
                      |    "confirmPageConfig": {
                      |      "showChangeLink": false,
                      |      "showSubHeadingAndInfo": true,
                      |      "showSearchAgainLink": true,
                      |      "showConfirmChangeText": false
                      |    },
                      |    "timeoutConfig": {
                      |      "timeoutAmount": 890,
                      |      "timeoutUrl": "${controllers.routes.JourneyRecoveryController.onPageLoad().url}"
                      |    }
                      |},
                      |    "labels": {
                      |      "en": {
                      |        "appLevelLabels": {
                      |          "navTitle": "${messagesApi("service.name")}"
                      |        },
                      |        "lookupPageLabels": {
                      |          "title": "${messagesApi("addressLookup.lookupPage.title")}",
                      |          "heading": "${messagesApi("addressLookup.lookupPage.heading", businessName)}"
                      |        },
                      |        "editPageLabels": {
                      |          "title": "${messagesApi("addressLookup.editPage.title")}",
                      |          "heading": "${messagesApi("addressLookup.editPage.heading", businessName)}"
                      |        },
                      |        "selectPageLabels": {
                      |           "title": "${messagesApi("addressLookup.selectPage.title")}",
                      |           "heading": "${messagesApi("addressLookup.selectPage.heading", businessName)}",
                      |           "headingWithPostcode": "foo",
                      |           "proposalListLabel": "Please select one of the following addresses",
                      |           "submitLabel": "Continue",
                      |           "searchAgainLinkText": "Search again"
                      |        },
                      |        "lookupPageLabels": {
                      |           "noResultsFoundMessage": "Sorry XXX, we couldn't find anything for that postcode."
                      |        }
                      |      }
                      |    }
                      |  }""".stripMargin

    Json.parse(v2Config).as[JsObject]
  }
}