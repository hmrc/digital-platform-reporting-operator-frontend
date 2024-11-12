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

package models.email.requests

import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import builders.UserAnswersBuilder.{aUserAnswers, anEmptyUserAnswer}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import pages.add.BusinessNamePage

class AddedReportingNotificationRequestSpec extends AnyFreeSpec
  with Matchers
  with TryValues
  with OptionValues
  with EitherValues {

  private val underTest = AddedReportingNotificationRequest

  ".apply(...)" - {
    "must create AddedReportingNotificationRequest object" in {
      AddedReportingNotificationRequest.apply("email@example.com", "first last", "some-business-name") mustBe AddedReportingNotificationRequest(
        to = List("email@example.com"),
        templateId = "dprs_added_reporting_notification",
        parameters = Map(
          "userPrimaryContactName" -> "first last",
          "poBusinessName" -> "some-business-name"
        )
      )
    }
  }

  ".build(...)" - {
    "must return correct AddedReportingNotificationRequest" in {
      val answers = anEmptyUserAnswer
        .set(BusinessNamePage, "some-business-name").success.value

      underTest.build(answers, aSubscriptionInfo) mustBe Right(AddedReportingNotificationRequest(
        to = List(aSubscriptionInfo.primaryContact.email),
        templateId = "dprs_added_reporting_notification",
        parameters = Map(
          "userPrimaryContactName" -> aSubscriptionInfo.primaryContactName,
          "poBusinessName" -> "some-business-name"
        )
      ))
    }

    "must return list of missing field errors when not found in user answers" in {
      val answers = aUserAnswers
        .remove(BusinessNamePage).success.value

      val result = underTest.build(answers, aSubscriptionInfo)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(BusinessNamePage)
    }
  }
}
