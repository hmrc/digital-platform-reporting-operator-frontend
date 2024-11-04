package models.email.requests

import builders.UserAnswersBuilder.{aUserAnswers, anEmptyUserAnswer}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import pages.add.{BusinessNamePage, PrimaryContactEmailPage, PrimaryContactNamePage}

class AddedPlatformOperatorRequestSpec extends AnyFreeSpec
  with Matchers
  with TryValues
  with OptionValues
  with EitherValues {

  private val underTest = AddedPlatformOperatorRequest

  ".apply(...)" - {
    "must create AddedPlatformOperatorRequest object" in {
      AddedPlatformOperatorRequest.apply("some.email@example.com", "some-name", "some-business-name", "some-po-id") mustBe AddedPlatformOperatorRequest(
        to = List("some.email@example.com"),
        templateId = "dprs_added_platform_operator",
        parameters = Map("userPrimaryContactName" -> "some-name",
          "poBusinessName" -> "some-business-name",
          "poId" -> "some-po-id")
      )
    }
  }

  ".build(...)" - {
    "must return correct AddedPlatformOperatorRequest" in {
      val answers = anEmptyUserAnswer.copy(operatorId = Some("some-operator-id"))
        .set(PrimaryContactEmailPage, "some@example.com").success.value
        .set(PrimaryContactNamePage, "some-name").success.value
        .set(BusinessNamePage, "some-business-name").success.value

      underTest.build(answers) mustBe Right(AddedPlatformOperatorRequest(
        to = List("some@example.com"),
        templateId = "dprs_added_platform_operator",
        parameters = Map(
          "userPrimaryContactName" -> "some-name",
          "poBusinessName" -> "some-business-name",
          "poId" -> "some-operator-id"
        )
      ))
    }

    "must return errors when business type is Sole trader and getIndividualContactName fails" in {
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
