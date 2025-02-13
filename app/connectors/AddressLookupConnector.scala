/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import config.{AddressLookupConfig, FrontendAppConfig}
import connectors.httpParsers.AddressLookupInitializationHttpParser.AddressLookupInitializationResponse
import connectors.httpParsers.ConfirmedAddressHttpParser.{ConfirmedAddressReads, ConfirmedAddressResponse}
import play.api.Logging
import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject()(
                                        appConfig: FrontendAppConfig,
                                        addressLookupConfig: AddressLookupConfig,
                                        httpClient: HttpClientV2
                                      )(implicit executionContext: ExecutionContext) extends Logging {

  def initialise(continueUrl: String, accessibilityFooterUrl: String, businessName: String)
                (implicit hc: HeaderCarrier, language: Lang): Future[AddressLookupInitializationResponse] = {
    val addressLookupUrl = url"${appConfig.addressLookupFrontendUrl}/api/v2/init"
    val addressConfig = Json.toJson(addressLookupConfig.config(continueUrl = s"$continueUrl", accessibilityFooterUrl = accessibilityFooterUrl, businessName = businessName))
    httpClient.post(addressLookupUrl)
      .withBody(addressConfig)
      .execute[AddressLookupInitializationResponse]
  }

  def getAddress(id: String)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[ConfirmedAddressResponse] = {
    val getAddressUrl = s"${appConfig.addressLookupFrontendUrl}/api/confirmed?id=$id"
    httpClient.get(url"$getAddressUrl").execute[ConfirmedAddressResponse]
  }

}
