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

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import uk.gov.hmrc.transitmovementsauditing.generators.ModelGenerators
import uk.gov.hmrc.transitmovementsauditing.models.*

class MetadataRequestSpec extends AnyFreeSpec with Matchers with ScalaCheckDrivenPropertyChecks with ModelGenerators {
  private val path = Gen.listOfN(10, Gen.alphaChar).map(_.mkString)
  "when MetadataRequest is serialized, return an appropriate JsObject" in forAll(
    path,
    arbitrary[MovementId],
    arbitrary[MessageId],
    arbitrary[EORINumber],
    arbitrary[MovementType],
    arbitrary[MessageType]
  ) {
    (path, movementId, messageId, eoriNumber, movementType, messageType) =>
      val actual = MetadataRequest.metadataRequestFormat.writes(
        MetadataRequest(path, Some(movementId), Some(messageId), Some(eoriNumber), Some(movementType), Some(messageType))
      )
      val expected = Json.obj(
        "path"          -> path,
        "movementId"    -> movementId,
        "messageId"     -> messageId,
        "enrolmentEORI" -> eoriNumber,
        "movementType"  -> movementType,
        "messageType"   -> messageType
      )
      actual mustBe expected
  }

  "when an appropriate JsObject is deserialized, return a MetadataRequest" in forAll(
    path,
    arbitrary[MovementId],
    arbitrary[MessageId],
    arbitrary[EORINumber],
    arbitrary[MovementType],
    arbitrary[MessageType]
  ) {
    (path, movementId, messageId, eoriNumber, movementType, messageType) =>
      val actual = MetadataRequest.metadataRequestFormat.reads(
        Json.obj(
          "path"          -> path,
          "movementId"    -> movementId,
          "messageId"     -> messageId,
          "enrolmentEORI" -> eoriNumber,
          "movementType"  -> movementType,
          "messageType"   -> messageType
        )
      )
      val expected = MetadataRequest(path, Some(movementId), Some(messageId), Some(eoriNumber), Some(movementType), Some(messageType))
      actual mustBe JsSuccess(expected)
  }

}
