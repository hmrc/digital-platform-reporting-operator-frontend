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

package controllers.update

import base.SpecBase
import connectors.PlatformOperatorConnector
import forms.RemovePlatformOperatorFormProvider
import models.UserAnswers
import models.audit.{AuditModel, RemovePlatformOperatorAuditEventModel}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.update.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.PlatformOperatorDeletedQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import views.html.update.RemovePlatformOperatorView

import scala.concurrent.Future

class RemovePlatformOperatorControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]
  private val mockAuditService = mock[AuditService]

  private val formProvider = new RemovePlatformOperatorFormProvider()
  private val businessName = "name"
  private val form = formProvider(businessName)

  private val baseAnswers = emptyUserAnswers.set(BusinessNamePage, businessName).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository, mockAuditService)
    super.beforeEach()
  }

  "RemovePlatformOperator Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemovePlatformOperatorController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemovePlatformOperatorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, operatorId, businessName)(request, messages(application)).toString
      }
    }

    "must remove the platform operator and redirect to Platform Operator Removed for a POST when the answer is yes" in {

      val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      when(mockConnector.removePlatformOperator(any())(any())) thenReturn Future.successful(Done)
      when(mockRepository.set(any())) thenReturn Future.successful(true)
      when(mockAuditService.sendAudit(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemovePlatformOperatorController.onSubmit(operatorId).url)
            .withFormUrlEncodedBody(("value", "true"))
        val businessName: String = "name"
        val auditType: String = "RemovePlatformOperator"
        val expectedAuditEvent = RemovePlatformOperatorAuditEventModel(businessName, operatorId)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PlatformOperatorRemovedController.onPageLoad(operatorId).url

        verify(mockConnector, times(1)).removePlatformOperator(eqTo(operatorId))(any())
        verify(mockRepository, times(1)).set(answersCaptor.capture())
        verify(mockAuditService, times(1)).sendAudit(
          eqTo(AuditModel[RemovePlatformOperatorAuditEventModel](auditType, expectedAuditEvent)))(any(),any(),any())

        val savedAnswers = answersCaptor.getValue
        savedAnswers.get(PlatformOperatorDeletedQuery).value mustEqual "name"
      }
    }

    "must redirect to Platform Operator for a POST when the answer is no" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository),
            bind[AuditService].toInstance(mockAuditService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.RemovePlatformOperatorController.onSubmit(operatorId).url)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PlatformOperatorController.onPageLoad(operatorId).url

        verify(mockConnector, never()).removePlatformOperator(any())(any())
        verify(mockRepository, never()).clear(any(), any())
        verify(mockAuditService, never()).sendAudit(any())(any(), any(), any())
      }
    }
  }
}
