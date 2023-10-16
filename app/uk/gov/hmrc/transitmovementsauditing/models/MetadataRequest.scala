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

package uk.gov.hmrc.transitmovementsauditing.models

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.mvc.Headers
import uk.gov.hmrc.transitmovementsauditing.config.Constants

object MetadataRequest {
  implicit lazy val metadataRequestFormat: OFormat[MetadataRequest] = Json.format[MetadataRequest]

  def apply(headers: Headers): MetadataRequest =
    MetadataRequest(
      headers.get(Constants.XAuditMetaPath).getOrElse(""),
      headers.get(Constants.XAuditMetaMovementId).map(MovementId(_)),
      headers.get(Constants.XAuditMetaMessageId).map(MessageId(_)),
      headers.get(Constants.XAuditMetaEORI).map(EORINumber(_)),
      headers.get(Constants.XAuditMetaMovementType).flatMap(MovementType.findByName(_)),
      headers.get(Constants.XAuditMetaMessageType).flatMap(MessageType.findByCode(_))
    )
}

case class MetadataRequest(
  path: String,
  movementId: Option[MovementId],
  messageId: Option[MessageId],
  enrolmentEORI: Option[EORINumber],
  movementType: Option[MovementType],
  messageType: Option[MessageType]
)
