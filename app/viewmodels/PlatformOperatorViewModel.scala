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

package viewmodels

import models.operator.responses.PlatformOperator

final case class PlatformOperatorViewModel(operatorId: String,
                                           operatorName: String,
                                           hasReportingNotifications: Boolean,
                                           hasSubmissions: Boolean,
                                           hasAssumedReports: Boolean)

object PlatformOperatorViewModel {

  def apply(operator: PlatformOperator,
            hasSubmissions: Boolean,
            hasAssumedReports: Boolean): PlatformOperatorViewModel = PlatformOperatorViewModel(
    operatorId = operator.operatorId,
    operatorName = operator.operatorName,
    hasReportingNotifications = operator.notifications.nonEmpty,
    hasSubmissions = hasSubmissions,
    hasAssumedReports = hasAssumedReports
  )
}
