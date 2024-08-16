package pages

import controllers.routes
import models.{CheckMode, NormalMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class $className$PageSpec extends AnyFreeSpec with Matchers {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id")

    "in Normal Mode" - {

      "must go to Index" in {

        $className$Page.nextPage(NormalMode, emptyAnswers) mustEqual routes.IndexController.onPageLoad()
      }
    }

    "in Check Mode" - {

      "must go to Check Answers" in {

        $className$Page.nextPage(CheckMode, emptyAnswers) mustEqual routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }
}