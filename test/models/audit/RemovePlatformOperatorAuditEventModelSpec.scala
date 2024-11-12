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

package models.audit

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class RemovePlatformOperatorAuditEventModelSpec extends AnyFreeSpec with Matchers {

  "Remove platform operator" in {
    val expected = Json.parse(
      """
        |{
        | "confirmRemoval":true,
        | "platformOperatorIdentifiers":{
        |   "businessName":"some-business-name",
        |   "platformOperatorId":"some-operator-id"}
        | }
        |""".stripMargin
    )

    val auditEvent = RemovePlatformOperatorAuditEventModel("some-business-name", "some-operator-id")

    Json.toJson(auditEvent) mustEqual expected
  }
}
