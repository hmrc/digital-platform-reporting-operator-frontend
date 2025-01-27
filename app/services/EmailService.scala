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

import cats.data.EitherNec
import com.google.inject.Inject
import connectors.{EmailConnector, SubscriptionConnector}
import models.UserAnswers
import models.email.EmailsSentResult
import models.email.requests.AddedAsPlatformOperatorRequest.AddedAsPlatformOperatorTemplateId
import models.email.requests.AddedAsReportingNotificationRequest.AddedAsReportingNotificationTemplateId
import models.email.requests.AddedPlatformOperatorRequest.AddedPlatformOperatorTemplateId
import models.email.requests.AddedReportingNotificationRequest.AddedReportingNotificationTemplateId
import models.email.requests.RemovedAsPlatformOperatorRequest.RemovedAsPlatformOperatorTemplateId
import models.email.requests.RemovedPlatformOperatorRequest.RemovedPlatformOperatorTemplateId
import models.email.requests.UpdatedAsPlatformOperatorRequest.UpdatedAsPlatformOperatorTemplateId
import models.email.requests.UpdatedPlatformOperatorRequest.UpdatedPlatformOperatorTemplateId
import models.email.requests._
import models.operator.requests.UpdatePlatformOperatorRequest
import pages.add.PrimaryContactEmailPage
import play.api.i18n.Lang.logger
import queries.Query
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject()(emailConnector: EmailConnector,
                             subscriptionConnector: SubscriptionConnector)
                            (implicit ec: ExecutionContext) {

  def sendAddPlatformOperatorEmails(userAnswers: UserAnswers)
                                   (implicit hc: HeaderCarrier): Future[EmailsSentResult] = (for {
    subscriptionInfo <- subscriptionConnector.getSubscriptionInfo
    emailSentResult <- {
      val poEmailSent = if (!matchingEmails(userAnswers, subscriptionInfo.primaryContact.email)) {
        Some(sendEmail(AddedAsPlatformOperatorRequest.build(userAnswers), AddedAsPlatformOperatorTemplateId))
      } else {
        None
      }
      val userEmailSent = sendEmail(AddedPlatformOperatorRequest.build(userAnswers, subscriptionInfo), AddedPlatformOperatorTemplateId)
      for {
        userResult <- userEmailSent
        poResult <- poEmailSent.map(_.map(Some(_))).getOrElse(Future.successful(None))
      } yield EmailsSentResult(userResult, poResult)
    }
  } yield emailSentResult).recover {
    case error => logger.warn("Add platform operator emails not sent", error)
      EmailsSentResult(userEmailSent = false, None)
  }

  def sendRemovePlatformOperatorEmails(userAnswers: UserAnswers)
                                      (implicit hc: HeaderCarrier): Future[EmailsSentResult] = (for {
    subscriptionInfo <- subscriptionConnector.getSubscriptionInfo
    emailSentResult <- {
      val poEmailSent = if (!matchingEmails(userAnswers, subscriptionInfo.primaryContact.email)) {
        Some(sendEmail(RemovedAsPlatformOperatorRequest.build(userAnswers), RemovedAsPlatformOperatorTemplateId))
      } else {
        None
      }
      val userEmailSent = sendEmail(RemovedPlatformOperatorRequest.build(userAnswers, subscriptionInfo), RemovedPlatformOperatorTemplateId)
      for {
        userResult <- userEmailSent
        poResult <- poEmailSent.map(_.map(Some(_))).getOrElse(Future.successful(None))
      } yield EmailsSentResult(userResult, poResult)
    }
  } yield emailSentResult).recover {
    case error => logger.warn("Remove platform operator emails not sent", error)
      EmailsSentResult(userEmailSent = false, None)
  }

  def sendUpdatedPlatformOperatorEmails(userAnswers: UserAnswers)
                                       (implicit hc: HeaderCarrier): Future[EmailsSentResult] = (for {
    subscriptionInfo <- subscriptionConnector.getSubscriptionInfo
    emailSentResult <- {
      val poEmailSent = if (!matchingEmails(userAnswers, subscriptionInfo.primaryContact.email)) {
        Some(sendEmail(UpdatedAsPlatformOperatorRequest.build(userAnswers), UpdatedAsPlatformOperatorTemplateId))
      } else {
        None
      }
      val userEmailSent = sendEmail(UpdatedPlatformOperatorRequest.build(userAnswers, subscriptionInfo), UpdatedPlatformOperatorTemplateId)
      for {
        userResult <- userEmailSent
        poResult <- poEmailSent.map(_.map(Some(_))).getOrElse(Future.successful(None))
      } yield EmailsSentResult(userResult, poResult)
    }
  } yield emailSentResult).recover {
    case error => logger.warn("Update platform operator emails not sent", error)
      EmailsSentResult(userEmailSent = false, None)
  }

  def sendAddReportingNotificationEmails(userAnswers: UserAnswers,
                                         addNotificationRequest: UpdatePlatformOperatorRequest)
                                        (implicit hc: HeaderCarrier): Future[EmailsSentResult] = (for {
    subscriptionInfo <- subscriptionConnector.getSubscriptionInfo
    emailSentResult <- {
      val poEmailSent = if (!matchingEmails(userAnswers, subscriptionInfo.primaryContact.email)) {
        Some(sendEmail(AddedAsReportingNotificationRequest.build(userAnswers, addNotificationRequest), AddedAsReportingNotificationTemplateId))
      } else {
        None
      }
      val userEmailSent = sendEmail(AddedReportingNotificationRequest.build(userAnswers, subscriptionInfo), AddedReportingNotificationTemplateId)
      for {
        userResult <- userEmailSent
        poResult <- poEmailSent.map(_.map(Some(_))).getOrElse(Future.successful(None))
      } yield EmailsSentResult(userResult, poResult)
    }
  } yield emailSentResult).recover {
    case error => logger.warn("Add notification for platform operator emails not sent", error)
      EmailsSentResult(userEmailSent = false, None)
  }

  private def matchingEmails(userAnswers: UserAnswers, poEmail: String): Boolean =
    userAnswers.get(PrimaryContactEmailPage).getOrElse("").trim.toLowerCase() == poEmail.trim.toLowerCase

  private def sendEmail(requestBuild: EitherNec[Query, SendEmailRequest], templateName: String)
                       (implicit hc: HeaderCarrier): Future[Boolean] = requestBuild.fold(
    errors => {
      logger.warn(s"Unable to send email ($templateName), path(s) missing:" +
        s"${errors.toChain.toList.map(_.path).mkString(", ")}")
      Future.failed(new RuntimeException(errors.toString))
    },
    request => emailConnector.send(request)
  )
}
