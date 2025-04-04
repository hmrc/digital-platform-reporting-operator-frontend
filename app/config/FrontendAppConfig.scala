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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration) {

  val host: String = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  val digitalPlatformReportingUrl: Service = configuration.get[Service]("microservice.services.digital-platform-reporting")
  val taxEnrolmentsBaseUrl: String = configuration.get[Service]("microservice.services.tax-enrolments").baseUrl

  val loginUrl: String = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String = configuration.get[String]("urls.signOut")

  private val exitSurveyBaseUrl: String = configuration.get[String]("feedback-frontend.host")
  val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/digital-platform-reporting-operator-frontend"

  val emailServiceUrl: String = configuration.get[Service]("microservice.services.email").baseUrl

  val timeout: Int = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val dataEncryptionEnabled: Boolean = configuration.get[Boolean]("features.use-encryption")

  val manageFrontendUrl: String = configuration.get[String]("microservice.services.digital-platform-reporting-manage-frontend.baseUrl")

  private val submissionFrontendUrl: String = configuration.get[String]("microservice.services.digital-platform-reporting-submission-frontend.baseUrl")

  def viewXMLSubmissions(operatorId: String) = s"$submissionFrontendUrl/submission/view?operatorId=$operatorId&reportingPeriod=0"

  def addXMLSubmissions(operatorId: String) = s"$submissionFrontendUrl/submission/$operatorId/start-page"

  def viewAssumedReporting(operatorId: String) = s"$submissionFrontendUrl/assumed-reporting/view?operatorId=$operatorId"

  def addAssumedReporting(operatorId: String) = s"$submissionFrontendUrl/assumed-reporting/$operatorId/start"
}
