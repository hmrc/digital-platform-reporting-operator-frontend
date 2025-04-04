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

package models.audit

import play.api.libs.json.{JsObject, Json, OWrites}

case class RemovePlatformOperatorAuditEventModel(businessName: String, operatorId: String){
  private def name = "RemovePlatformOperator"
  def toAuditModel: AuditModel[RemovePlatformOperatorAuditEventModel] = {
    AuditModel(name, this)
  }
}

object RemovePlatformOperatorAuditEventModel {

  implicit lazy val writes: OWrites[RemovePlatformOperatorAuditEventModel] = new OWrites[RemovePlatformOperatorAuditEventModel] {
    override def writes(o: RemovePlatformOperatorAuditEventModel): JsObject = {
      Json.obj(
        "confirmRemoval" -> true,
        "platformOperatorIdentifiers" -> Json.obj(
          "businessName" -> o.businessName,
          "platformOperatorId" -> o.operatorId
        )
      )
    }
  }

}