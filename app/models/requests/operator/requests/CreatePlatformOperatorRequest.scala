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

package models.requests.operator.requests

import models.requests.operator.{AddressDetails, ContactDetails, TinDetails}
import play.api.libs.json.{Json, OFormat}

final case class CreatePlatformOperatorRequest(
                                                subscriptionId: String,
                                                operatorName: String,
                                                tinDetails: Seq[TinDetails],
                                                businessName: Option[String],
                                                tradingName: Option[String],
                                                primaryContactDetails: ContactDetails,
                                                secondaryContactDetails: Option[ContactDetails],
                                                addressDetails: AddressDetails
                                              )

object CreatePlatformOperatorRequest {

  implicit lazy val format: OFormat[CreatePlatformOperatorRequest] = Json.format
}
