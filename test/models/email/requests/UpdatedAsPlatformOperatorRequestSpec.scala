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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}

class UpdatedAsPlatformOperatorRequestSpec extends AnyFreeSpec
  with Matchers
  with TryValues
  with OptionValues
  with EitherValues {

  private val underTest = UpdatedAsPlatformOperatorRequest

  ".apply(...)" - {
    "must create UpdatedAsPlatformOperatorRequest object" in {
      UpdatedAsPlatformOperatorRequest.apply("some.email@example.com", "some-name", "some-business-name") mustBe UpdatedAsPlatformOperatorRequest(
        to = List("some.email@example.com"),
        templateId = "dprs_updated_as_platform_operator",
        parameters = Map(
          "poPrimaryContactName" -> "some-name",
          "poBusinessName" -> "some-business-name"
        )
      )
    }
  }

  ".build(...)" - {
    "must return correct UpdatedAsPlatformOperatorRequest" in {
      val answers = anEmptyUserAnswer
        .set(PrimaryContactEmailPage, "some@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value

      underTest.build(answers) mustBe Right(UpdatedAsPlatformOperatorRequest(
        to = List("some@example.com"),
        templateId = "dprs_updated_as_platform_operator",
        parameters = Map(
          "poPrimaryContactName" -> "some-name",
          "poBusinessName" -> "some-business-name"
        )
      ))
    }

    "must return list of missing field errors when not found in user answers" in {
      val answers = aUserAnswers
        .remove(PrimaryContactEmailPage).success.value
        .remove(PrimaryContactNamePage).success.value
        .remove(BusinessNamePage).success.value

      val result = underTest.build(answers)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(
        PrimaryContactEmailPage,
        PrimaryContactNamePage,
        BusinessNamePage
      )
    }
  }
}
