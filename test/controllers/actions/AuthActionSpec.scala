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

package controllers.actions

import auth.Retrievals._
import base.SpecBase
import builders.PendingEnrolmentBuilder.aPendingEnrolment
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.PendingEnrolmentConnector
import controllers.routes
import models.eacd.EnrolmentDetails
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EnrolmentService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AuthActionSpec extends SpecBase
  with MockitoSugar
  with BeforeAndAfterEach {

  private val application = applicationBuilder(userAnswers = None).build()
  private val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
  private val appConfig = application.injector.instanceOf[FrontendAppConfig]
  private val emptyEnrolments = Enrolments(Set.empty)
  private val mockPendingEnrolmentConnector = mock[PendingEnrolmentConnector]
  private val mockEnrolmentService = mock[EnrolmentService]

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Results.Ok(s"${request.userId} ${request.dprsId}")
    }
  }

  override def beforeEach(): Unit = {
    reset(mockPendingEnrolmentConnector, mockEnrolmentService)
    super.beforeEach()
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken),
          appConfig,
          bodyParsers,
          mockPendingEnrolmentConnector,
          mockEnrolmentService)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must startWith(appConfig.loginUrl)
      }
    }

    "when the user's session has expired" - {

      "must redirect the user to log in " in {

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired),
          appConfig,
          bodyParsers,
          mockPendingEnrolmentConnector,
          mockEnrolmentService)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must startWith(appConfig.loginUrl)
      }
    }

    "when InsufficientEnrolments" - {
      "must redirect the user to unauthorised page" - {
        "if no pending enrolment is found" in {
          when(mockPendingEnrolmentConnector.getPendingEnrolment()(any())).thenReturn(Future.failed(new RuntimeException()))

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(InsufficientEnrolments("error")),
            appConfig,
            bodyParsers,
            mockPendingEnrolmentConnector,
            mockEnrolmentService)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url

          verify(mockEnrolmentService, never).enrol(any())(any())
          verify(mockPendingEnrolmentConnector, never).remove()(any())
        }

        "if enrolment fails" in {
          when(mockPendingEnrolmentConnector.getPendingEnrolment()(any())).thenReturn(Future.successful(aPendingEnrolment))
          when(mockEnrolmentService.enrol(eqTo(EnrolmentDetails(aPendingEnrolment)))(any())).thenReturn(Future.failed(new RuntimeException()))

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(InsufficientEnrolments("error")),
            appConfig,
            bodyParsers,
            mockPendingEnrolmentConnector,
            mockEnrolmentService)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())


          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url

          verify(mockPendingEnrolmentConnector, never).remove()(any())
        }
      }
    }

    "when AuthorisationException" - {
      "must redirect to unauthorised page" in {
        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(InternalError("error")),
          appConfig,
          bodyParsers,
          mockPendingEnrolmentConnector,
          mockEnrolmentService)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url

        verify(mockPendingEnrolmentConnector, never).getPendingEnrolment()(any())
        verify(mockEnrolmentService, never).enrol(any())(any())
        verify(mockPendingEnrolmentConnector, never).remove()(any())
      }
    }

    "when the user doesn't have a DPRS enrolments" - {

      "must redirect the user to the unauthorised page" in {
        when(mockPendingEnrolmentConnector.getPendingEnrolment()(any())).thenReturn(Future.failed(new RuntimeException()))

        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(Some("internalId") ~ emptyEnrolments),
          appConfig,
          bodyParsers,
          mockPendingEnrolmentConnector,
          mockEnrolmentService)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url

        verify(mockEnrolmentService, never).enrol(any())(any())
        verify(mockPendingEnrolmentConnector, never).remove()(any())
      }
    }

    "when the user has a DPRS enrolment" - {

      "must succeed" in {

        val enrolments = Enrolments(Set(Enrolment("HMRC-DPRS", Seq(EnrolmentIdentifier("DPRSID", "dprsId")), "activated", None)))
        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(Some("internalId") ~ enrolments),
          appConfig,
          bodyParsers,
          mockPendingEnrolmentConnector,
          mockEnrolmentService)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustEqual "internalId dprsId"

        verify(mockEnrolmentService, never).enrol(any())(any())
        verify(mockPendingEnrolmentConnector, never).remove()(any())
      }
    }
  }
}

class FakeAuthConnector[T](value: T) extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.fromTry(Try(value.asInstanceOf[A]))
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
