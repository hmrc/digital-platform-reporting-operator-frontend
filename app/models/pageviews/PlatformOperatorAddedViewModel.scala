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

import models.email.EmailsSentResult
import models.subscription.SubscriptionInfo
import viewmodels.PlatformOperatorSummaryViewModel

case class PlatformOperatorAddedViewModel(userEmail: String,
                                          operatorId: String,
                                          poBusinessName: String,
                                          poEmail: String,
                                          emailsSentResult: EmailsSentResult) {

  lazy val sentEmails: Seq[String] = Seq(
    if (emailsSentResult.userEmailSent) Some(userEmail) else None,
    emailsSentResult.poEmailSent.filter(identity).map(_ => poEmail)
  ).flatten
}

object PlatformOperatorAddedViewModel {

  def apply(subscriptionInfo: SubscriptionInfo,
            platformOperatorSummaryViewModel: PlatformOperatorSummaryViewModel,
            emailsSentResult: EmailsSentResult): PlatformOperatorAddedViewModel = PlatformOperatorAddedViewModel(
    userEmail = subscriptionInfo.primaryContact.email,
    operatorId = platformOperatorSummaryViewModel.operatorId,
    poBusinessName = platformOperatorSummaryViewModel.operatorName,
    poEmail = platformOperatorSummaryViewModel.poPrimaryContactEmail,
    emailsSentResult = emailsSentResult
  )
}
