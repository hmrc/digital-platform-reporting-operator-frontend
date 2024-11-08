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

import builders.UserAnswersBuilder.{aUserAnswers, anEmptyUserAnswer}
import models.operator.NotificationType.Rpo
import models.operator.{AddressDetails, ContactDetails}
import models.operator.requests.{Notification, UpdatePlatformOperatorRequest}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}
import pages.notification.ReportingPeriodPage

class AddedAsReportingNotificationRequestSpec extends AnyFreeSpec
  with Matchers
  with TryValues
  with OptionValues
  with EitherValues {

  private val underTest = AddedAsReportingNotificationRequest

  ".apply(...)" - {
    "must create AddedAsReportingNotificationRequest object" in {
      AddedAsReportingNotificationRequest.apply(
        "some.email@example.com", "some-name", isReportingPO = true, 2024, "some-business-name", isExtendedDueDiligence = true, isActiveSellerDueDiligence = true) mustBe AddedAsReportingNotificationRequest(
        to = List("some.email@example.com"),
        templateId = "dprs_added_reporting_notification_for_you",
        parameters = Map(
          "poPrimaryContactName" -> "some-name",
          "isReportingPO" -> "true",
          "reportablePeriodYear" -> "2024",
          "poBusinessName" -> "some-business-name",
          "isExtendedDueDiligence" -> "true",
          "isActiveSellerDueDiligence" -> "true"
        )
      )
    }
  }

  ".build(...)" - {

    val updatedPlatformOperatorRequest = UpdatePlatformOperatorRequest(subscriptionId = "dprs id",
      operatorId = "operatorId",
      operatorName = "name",
      tinDetails = Seq.empty,
      businessName = None,
      tradingName = None,
      primaryContactDetails = ContactDetails(None, "name", "email"),
      secondaryContactDetails = None,
      addressDetails = AddressDetails("line 1", None, None, None, None, None),
      notification = Some(Notification(Rpo, Some(true), Some(true), 2024))
    )

    "must return correct AddedAsReportingNotificationRequest" in {
      val answers = anEmptyUserAnswer.copy(operatorId = Some("some-operator-id"))
        .set(PrimaryContactEmailPage, "some@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value
        .set(ReportingPeriodPage, 2024).success.value


      underTest.build(answers, updatedPlatformOperatorRequest) mustBe Right(AddedAsReportingNotificationRequest(
        to = List("some@example.com"),
        templateId = "dprs_added_reporting_notification_for_you",
        parameters = Map(
          "poPrimaryContactName" -> "some-name",
          "isReportingPO" -> "true",
          "reportablePeriodYear" -> "2024",
          "poBusinessName" -> "some-business-name",
          "isExtendedDueDiligence" -> "true",
          "isActiveSellerDueDiligence" -> "true"
        )
      ))
    }

    "must return list of missing field errors when not found in user answers" in {
      val answers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(ReportingPeriodPage).success.value
        .remove(BusinessNamePage).success.value

      val result = underTest.build(answers, updatedPlatformOperatorRequest)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(
        PrimaryContactEmailPage,
        PrimaryContactNamePage,
        ReportingPeriodPage,
        BusinessNamePage
      )
    }

  }
}
