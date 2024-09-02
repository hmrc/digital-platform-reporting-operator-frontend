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

package pages.add

import controllers.add.routes
import models.{BusinessType, CheckMode, NormalMode, UkTaxIdentifiers, UserAnswers}
import models.UkTaxIdentifiers._
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class UkTaxIdentifiersPageSpec
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaCheckPropertyChecks {

  private val emptyAnswers = UserAnswers("id")

  ".nextPage" - {

    "in Normal Mode" - {

      "must go to Business Type when UTR is chosen" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet + Utr

        forAll(identifierGen) { identifiers =>

          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UkTaxIdentifiersPage.nextPage(NormalMode, answers) mustEqual routes.BusinessTypeController.onPageLoad(NormalMode)
        }
      }

      "must go to CRN when CRN is selected and UTR is not selected" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet + Crn - Utr

        forAll(identifierGen) { identifiers =>

          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UkTaxIdentifiersPage.nextPage(NormalMode, answers) mustEqual routes.CrnController.onPageLoad(NormalMode)
        }
      }

      "must go to VRN when VRN is selected and neither UTR nor CRN is selected" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet + Vrn - Crn - Utr

        forAll(identifierGen) { identifiers =>

          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UkTaxIdentifiersPage.nextPage(NormalMode, answers) mustEqual routes.VrnController.onPageLoad(NormalMode)
        }
      }

      "must go to EMPREF when EMPREF is selected and neither UTR, CRN nor VRN is selected" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          maybeChrn <- Gen.option(Chrn)
        } yield maybeChrn.map(Set[UkTaxIdentifiers](_)).getOrElse(Set.empty[UkTaxIdentifiers]) + Empref

        forAll(identifierGen) { identifiers =>

          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UkTaxIdentifiersPage.nextPage(NormalMode, answers) mustEqual routes.EmprefController.onPageLoad(NormalMode)
        }
      }

      "must go to CHRN when only CHRN is selected" in {

        val answers = emptyAnswers.set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Chrn)).success.value
        UkTaxIdentifiersPage.nextPage(NormalMode, answers) mustEqual routes.ChrnController.onPageLoad(NormalMode)
      }
    }

    "in Check Mode" - {

      "must go to Business Type when UTR is selected and Business Type is not answered" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet + Utr

        forAll(identifierGen) { identifiers =>

          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.BusinessTypeController.onPageLoad(CheckMode)
        }
      }

      "must go to CRN when CRN is selected and has not been answered" - {

        "and UTR is not selected" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Crn - Utr

          forAll(identifierGen) { identifiers =>

            val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
            UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.CrnController.onPageLoad(CheckMode)
          }
        }

        "and UTR is selected and business type has been answered" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Crn + Utr

          forAll(identifierGen) { identifiers =>

            val answers =
              emptyAnswers
                .set(UkTaxIdentifiersPage, identifiers).success.value
                .set(BusinessTypePage, BusinessType.Llp).success.value

            UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.CrnController.onPageLoad(CheckMode)
          }
        }
      }

      "must go to VRN when VRN is selected and has not been answered" - {

        "and UTR and CRN are not selected" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Vrn - Crn - Utr

          forAll(identifierGen) { identifiers =>

            val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
            UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.VrnController.onPageLoad(CheckMode)
          }
        }

        "and UTR and CRN are selected, and Business Type and CRN are answered" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Vrn + Crn + Utr

          forAll(identifierGen) { identifiers =>

            val answers =
              emptyAnswers
                .set(UkTaxIdentifiersPage, identifiers).success.value
                .set(BusinessTypePage, BusinessType.Llp).success.value
                .set(CrnPage, "crn").success.value

            UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.VrnController.onPageLoad(CheckMode)
          }
        }
      }

      "must go to EMPREF when EMPREF is selected and has not been answered" - {

        "and UTR, CRN and VRN are not selected" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Empref - Crn - Utr - Vrn

          forAll(identifierGen) { identifiers =>

            val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
            UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.EmprefController.onPageLoad(CheckMode)
          }
        }

        "and UTR, CRN and VRN are selected and Business Type, CRN and VRN are answered" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Empref + Vrn + Crn + Utr

          forAll(identifierGen) { identifiers =>

            val answers =
              emptyAnswers
                .set(UkTaxIdentifiersPage, identifiers).success.value
                .set(BusinessTypePage, BusinessType.Llp).success.value
                .set(CrnPage, "crn").success.value
                .set(VrnPage, "vrn").success.value

            UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.EmprefController.onPageLoad(CheckMode)
          }
        }
      }

      "must go to CHRN when CHRN is selected and has not been answered" - {

        "and UTR, CRN, VRN and EMPREF are not selected" in {

          val answers = emptyAnswers.set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Chrn)).success.value
          UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.ChrnController.onPageLoad(CheckMode)
        }

        "and UTR, CRN, VRN and EMPREF are selected and Business Type, CRN, VRN and EMPREF are answered" in {

          val answers =
            emptyAnswers
              .set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr, Crn, Vrn, Empref, Chrn)).success.value
              .set(BusinessTypePage, BusinessType.Llp).success.value
              .set(CrnPage, "crn").success.value
              .set(VrnPage, "vrn").success.value
              .set(EmprefPage, "empref").success.value

          UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.ChrnController.onPageLoad(CheckMode)
        }
      }

      "must go to Check Answers" - {

        "when all selected options have been answered" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet

          forAll(identifierGen) { identifiers =>

            val baseAnswers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value

            val answers = identifiers.foldLeft(baseAnswers) { (acc, next) =>
              next match {
                case Utr    => acc.set(BusinessTypePage, BusinessType.Partnership).success.value
                case Crn    => acc.set(CrnPage, "crn").success.value
                case Vrn    => acc.set(VrnPage, "vrn").success.value
                case Empref => acc.set(EmprefPage, "empref").success.value
                case Chrn   => acc.set(ChrnPage, "chrn").success.value
              }
            }

            UkTaxIdentifiersPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad()
          }
        }
      }
    }
  }

  ".cleanup" - {

    val baseAnswers =
      emptyAnswers
        .set(BusinessTypePage, BusinessType.Llp).success.value
        .set(UtrPage, "utr").success.value
        .set(CrnPage, "crn").success.value
        .set(VrnPage, "vrn").success.value
        .set(EmprefPage, "empref").success.value
        .set(ChrnPage, "chrn").success.value

    "must remove Business Type and UTR when UTR is not selected" in {

      val result = baseAnswers.set(UkTaxIdentifiersPage, values.toSet - Utr).success.value

      result.get(BusinessTypePage) must not be defined
      result.get(UtrPage)          must not be defined
      result.get(CrnPage)          mustBe defined
      result.get(VrnPage)          mustBe defined
      result.get(EmprefPage)       mustBe defined
      result.get(ChrnPage)         mustBe defined
    }

    "must remove CRN when CRN is not selected" in {

      val result = baseAnswers.set(UkTaxIdentifiersPage, values.toSet - Crn).success.value

      result.get(CrnPage)          must not be defined
      result.get(BusinessTypePage) mustBe defined
      result.get(UtrPage)          mustBe defined
      result.get(VrnPage)          mustBe defined
      result.get(EmprefPage)       mustBe defined
      result.get(ChrnPage)         mustBe defined
    }

    "must remove VRN when VRN is not selected" in {

      val result = baseAnswers.set(UkTaxIdentifiersPage, values.toSet - Vrn).success.value

      result.get(VrnPage)          must not be defined
      result.get(BusinessTypePage) mustBe defined
      result.get(UtrPage)          mustBe defined
      result.get(CrnPage)          mustBe defined
      result.get(EmprefPage)       mustBe defined
      result.get(ChrnPage)         mustBe defined
    }

    "must remove EMPREF when EMPREF is not selected" in {

      val result = baseAnswers.set(UkTaxIdentifiersPage, values.toSet - Empref).success.value

      result.get(EmprefPage)       must not be defined
      result.get(BusinessTypePage) mustBe defined
      result.get(UtrPage)          mustBe defined
      result.get(CrnPage)          mustBe defined
      result.get(VrnPage)          mustBe defined
      result.get(ChrnPage)         mustBe defined
    }

    "must remove CHRN when CHRN is not selected" in {

      val result = baseAnswers.set(UkTaxIdentifiersPage, values.toSet - Chrn).success.value

      result.get(ChrnPage)         must not be defined
      result.get(BusinessTypePage) mustBe defined
      result.get(UtrPage)          mustBe defined
      result.get(CrnPage)          mustBe defined
      result.get(VrnPage)          mustBe defined
      result.get(EmprefPage)       mustBe defined
    }
  }
}
