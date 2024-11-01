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

import base.SpecBase
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}

class SendEmailRequestSpec extends SpecBase {

  "AddedPlatformOperatorRequest" - {
    ".apply - must create AddedPlatformOperatorRequest object" in {
      AddedPlatformOperatorRequest.apply("some.email@example.com",
        "some-name",
        "some-business-name",
        "some-po-id") mustBe AddedPlatformOperatorRequest(
        to = List("some.email@example.com"),
        templateId = "dprs_added_platform_operator",
        parameters = Map("userPrimaryContactName" -> "some-name",
          "poBusinessName" -> "some-business-name",
          "poId" -> "some-po-id")
      )
    }

    ".build - must return correct AddedPlatformOperatorRequest" in {
      val answers = emptyUserAnswers.copy(operatorId = Some("1"))
        .set(PrimaryContactEmailPage, "test@test.com").success.value
        .set(PrimaryContactNamePage, "John Doe").success.value
        .set(BusinessNamePage, "business name").success.value

      AddedPlatformOperatorRequest.build(answers) mustBe Right(AddedPlatformOperatorRequest(
        to = List("test@test.com"),
        templateId = "dprs_added_platform_operator",
        parameters = Map("userPrimaryContactName" -> "John Doe", "poBusinessName" -> "business name","poId" -> "1")
      ))
    }
  }
}
