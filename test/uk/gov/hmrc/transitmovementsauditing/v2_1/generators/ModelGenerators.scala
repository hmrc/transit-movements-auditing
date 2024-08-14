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

package uk.gov.hmrc.transitmovementsauditing.v2_1.generators

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import uk.gov.hmrc.objectstore.client.Md5Hash
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.Channel
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.ClientId
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.EORINumber
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.MessageId
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.MessageType
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.Metadata
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.MovementId
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.MovementType
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.request.MetadataRequest

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

trait ModelGenerators {

  implicit lazy val arbitraryObjectSummaryWithMd5: Arbitrary[ObjectSummaryWithMd5] = Arbitrary {
    for {
      fileId <- Gen.alphaNumStr
      lastModified      = Instant.now()
      formattedDateTime = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(lastModified)
      contentLen <- Gen.long
      hash       <- Gen.alphaNumStr.map(Md5Hash)
    } yield ObjectSummaryWithMd5(
      Path.Directory("folder1/folder2").file(s"$fileId-$formattedDateTime.xml"),
      contentLen,
      hash,
      lastModified
    )
  }

  lazy val genShortUUID: Gen[String] = Gen.long.map {
    l: Long =>
      f"${BigInt(l)}%016x"
  }

  implicit lazy val arbitraryMessageId: Arbitrary[MessageId] = Arbitrary {
    genShortUUID.map(MessageId(_))
  }

  implicit lazy val arbitraryEORINumber: Arbitrary[EORINumber] = Arbitrary {
    Gen.alphaNumStr.map(EORINumber(_))
  }

  implicit lazy val arbitraryMessageType: Arbitrary[MessageType] = Arbitrary {
    Gen.oneOf(MessageType.values)
  }

  implicit lazy val arbitraryAuditType: Arbitrary[AuditType] = Arbitrary {
    Gen.oneOf(AuditType.values)
  }

  implicit lazy val arbitraryMovementId: Arbitrary[MovementId] = Arbitrary {
    genShortUUID.map(MovementId(_))
  }

  implicit lazy val arbitraryMovementType: Arbitrary[MovementType] = Arbitrary {
    Gen.oneOf(MovementType.values)
  }

  implicit lazy val arbitraryClientId: Arbitrary[ClientId] = Arbitrary {
    Gen.stringOfN(24, Gen.alphaNumChar).map(ClientId.apply)
  }

  implicit lazy val arbitraryChannel: Arbitrary[Channel] = Arbitrary {
    Gen.oneOf(Channel.values)
  }

  implicit val arbitraryMetadata: Arbitrary[Metadata] = Arbitrary {
    for {
      path          <- Gen.alphaNumStr
      movementId    <- arbitrary[MovementId]
      messageId     <- arbitrary[MessageId]
      enrolmentEORI <- arbitrary[EORINumber]
      movementType  <- arbitrary[MovementType]
      messageType   <- arbitrary[MessageType]
      clientId      <- arbitrary[ClientId]
      channel       <- arbitrary[Channel]
    } yield Metadata(path, Some(movementId), Some(messageId), Some(enrolmentEORI), Some(movementType), Some(messageType), Some(clientId), Some(channel))
  }

  implicit val arbitraryMetadataRequest: Arbitrary[MetadataRequest] = Arbitrary {
    for {
      path          <- Gen.alphaNumStr
      movementId    <- arbitrary[MovementId]
      messageId     <- arbitrary[MessageId]
      enrolmentEORI <- arbitrary[EORINumber]
      movementType  <- arbitrary[MovementType]
      messageType   <- arbitrary[MessageType]
    } yield MetadataRequest(path, Some(movementId), Some(messageId), Some(enrolmentEORI), Some(movementType), Some(messageType))
  }
}
