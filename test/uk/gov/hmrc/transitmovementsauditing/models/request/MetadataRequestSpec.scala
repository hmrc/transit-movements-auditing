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

package uk.gov.hmrc.transitmovementsauditing.models.request

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.test.FakeHeaders
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.transitmovementsauditing.models.EORINumber
import uk.gov.hmrc.transitmovementsauditing.models.MessageId
import uk.gov.hmrc.transitmovementsauditing.models.MessageType
import uk.gov.hmrc.transitmovementsauditing.models.MovementId
import uk.gov.hmrc.transitmovementsauditing.models.MovementType

class MetadataRequestSpec extends AnyFreeSpec with Matchers {

  "Metadata with some optional headers" in {
    val headers = FakeHeaders(Seq(Constants.XAuditMetaPath -> "abc", Constants.XAuditMetaMovementId -> "123"))
    MetadataRequest.apply(headers) mustBe MetadataRequest("abc", Some(MovementId("123")), None, None, None, None)
  }

  "Metadata with all optional headers" in {
    val headers = FakeHeaders(
      Seq(
        Constants.XAuditMetaPath         -> "abc",
        Constants.XAuditMetaMovementId   -> "123",
        Constants.XAuditMetaMessageId    -> "123",
        Constants.XAuditMetaEORI         -> "ABC12",
        Constants.XAuditMetaMovementType -> MovementType.Arrival.movementType,
        Constants.XAuditMetaMessageType  -> MessageType.IE015.messageCode
      )
    )
    MetadataRequest.apply(headers) mustBe MetadataRequest(
      "abc",
      Some(MovementId("123")),
      Some(MessageId("123")),
      Some(EORINumber("ABC12")),
      Some(MovementType.Arrival),
      Some(MessageType.IE015)
    )
  }
}
