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

package uk.gov.hmrc.transitmovementsauditing.config

object Constants {
  val XContentLengthHeader   = "X-ContentLength"
  val XAuditSourceHeader     = "X-Audit-Source"
  val XAuditMetaPath         = "X-Audit-Meta-Path"
  val XAuditMetaMovementId   = "X-Audit-Meta-Movement-Id"
  val XAuditMetaMessageId    = "X-Audit-Meta-Message-Id"
  val XAuditMetaEORI         = "X-Audit-Meta-EORI"
  val XAuditMetaMovementType = "X-Audit-Meta-Movement-Type"
  val XAuditMetaMessageType  = "X-Audit-Meta-Message-Type"
}
