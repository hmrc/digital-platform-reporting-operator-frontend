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

package pages.update

import controllers.update.routes
import models.{Country, JerseyGuernseyIoMAddress, UkAddress, UserAnswers}
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class JerseyGuernseyIoMAddressPageSpec extends AnyFreeSpec with Matchers {

  private val emptyAnswers = UserAnswers("id")
  private val operatorId = "operatorId"

  private val ukAddress = UkAddress("line 1", None, "uk town", None, "AA1 1AA", Country("GB", "United Kingdom"))
  private val jerseyGuernseyIoMAddress = JerseyGuernseyIoMAddress("line 2", None, "guernsey town", None, "BB2 2BB", Country("GG", "Guernsey"))

  ".nextPage" - {

    "must go to Check Answers" in {

      JerseyGuernseyIoMAddressPage.nextPage(operatorId, emptyAnswers) mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId)
    }
  }

  ".cleanup" - {

    "must remove Uk address when the answer is JerseyGuernseyIoM" in {
      val answers =
        emptyAnswers
          .set(UkAddressPage, ukAddress).success.value

      val result = answers.set(JerseyGuernseyIoMAddressPage, jerseyGuernseyIoMAddress).success.value

      result.get(UkAddressPage) must not be defined
      result.get(JerseyGuernseyIoMAddressPage) mustBe defined
    }
  }
}