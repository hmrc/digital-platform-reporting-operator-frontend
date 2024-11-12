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

package services

import config.FrontendAppConfig
import models.audit.AuditModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditServiceSpec extends AnyFreeSpec
  with Matchers
  with MockitoSugar
  with FutureAwaits
  with DefaultAwaitTimeout {

  private val mockAuditConnector = mock[AuditConnector]
  private val mockAppConfig = mock[FrontendAppConfig]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val auditType = "some-audit-type"
  private val eventDetails = Json.obj("some-key" -> "some-details")

  private val underTest = new AuditService(mockAuditConnector, mockAppConfig)

  ".sendAudit" - {
    "create extended event and send to auditConnector" in {
      val event = AuditModel(auditType, eventDetails)

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      await(underTest.sendAudit(event)) mustBe AuditResult.Success

      verify(mockAppConfig, times(1)).appName
    }
  }
}