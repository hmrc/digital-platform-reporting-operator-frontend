/*
 * Copyright 2025 HM Revenue & Customs
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

package models.pageviews

import builders.EmailsSentResultBuilder.anEmailsSentResult
import builders.PlatformOperatorRemovedViewModelBuilder.aPlatformOperatorRemovedViewModel
import builders.PlatformOperatorSummaryViewModelBuilder.aPlatformOperatorSummaryViewModel
import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import models.email.EmailsSentResult
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class PlatformOperatorRemovedViewModelSpec extends AnyFreeSpec with Matchers {

  ".apply(...)" - {
    "must create correct object" in {
      PlatformOperatorRemovedViewModel.apply(
        subscriptionInfo = aSubscriptionInfo,
        platformOperatorSummaryViewModel = aPlatformOperatorSummaryViewModel,
        emailsSentResult = anEmailsSentResult,
      ) mustBe PlatformOperatorRemovedViewModel(
        userEmail = aSubscriptionInfo.primaryContact.email,
        operatorId = aPlatformOperatorSummaryViewModel.operatorId,
        poBusinessName = aPlatformOperatorSummaryViewModel.operatorName,
        poEmail = aPlatformOperatorSummaryViewModel.poPrimaryContactEmail,
        emailsSentResult = anEmailsSentResult
      )
    }
  }

  ".sentEmails(...)" - {
    "must return empty list when" - {
      "user and po emails failed" in {
        val underTest = aPlatformOperatorRemovedViewModel.copy(emailsSentResult = EmailsSentResult(userEmailSent = false, Some(false)))

        underTest.sentEmails mustBe Seq.empty
      }

      "user email failed and no po email" in {
        val underTest = aPlatformOperatorRemovedViewModel.copy(emailsSentResult = EmailsSentResult(userEmailSent = false, None))

        underTest.sentEmails mustBe Seq.empty
      }
    }

    "must return user email only when" - {
      "user email sent and no po email info" in {
        val underTest = aPlatformOperatorRemovedViewModel.copy(emailsSentResult = EmailsSentResult(userEmailSent = true, None))

        underTest.sentEmails mustBe Seq(aPlatformOperatorRemovedViewModel.userEmail)
      }

      "user email sent and no po email failed" in {
        val underTest = aPlatformOperatorRemovedViewModel.copy(emailsSentResult = EmailsSentResult(userEmailSent = true, Some(false)))

        underTest.sentEmails mustBe Seq(aPlatformOperatorRemovedViewModel.userEmail)
      }
    }

    "must return po email only when" - {
      "user email failed and po email sent" in {
        val underTest = aPlatformOperatorRemovedViewModel.copy(emailsSentResult = EmailsSentResult(userEmailSent = false, Some(true)))

        underTest.sentEmails mustBe Seq(aPlatformOperatorRemovedViewModel.poEmail)
      }
    }

    "must return user an po emails when both succeed" in {
      val underTest = aPlatformOperatorRemovedViewModel.copy(emailsSentResult = EmailsSentResult(userEmailSent = true, Some(true)))

      underTest.sentEmails mustBe Seq(aPlatformOperatorRemovedViewModel.userEmail, aPlatformOperatorRemovedViewModel.poEmail)
    }
  }
}
