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

package uk.gov.hmrc.transitmovementsauditing.v2_1.models.request

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{JsObject, JsSuccess, Json}
import uk.gov.hmrc.transitmovementsauditing.v2_1.generators.ModelGenerators

class DetailsRequestSpec extends AnyFreeSpec with Matchers with ScalaCheckDrivenPropertyChecks with ModelGenerators {
  implicit val jsValueArbitrary: Arbitrary[JsObject] = Arbitrary(Gen.const(Json.obj("code" -> "BUSINESS_VALIDATION_ERROR", "message" -> "Expected NTA.GB")))
  "when DetailsRequest is serialized, return an appropriate JsObject" in forAll(
    arbitrary[MetadataRequest],
    arbitrary[JsObject]
  ) {
    (metadata, payload) =>
      val actual = DetailsRequest.detailsRequestFormat.writes(DetailsRequest(metadata, Some(payload)))
      val expected = Json.obj(
        "metadata" -> metadata,
        "payload"  -> payload
      )
      actual mustBe expected
  }

  "when an appropriate JsObject is deserialized, return a DetailsRequest" in forAll(
    arbitrary[MetadataRequest],
    arbitrary[JsObject]
  ) {
    (metadata, payload) =>
      val actual = DetailsRequest.detailsRequestFormat.reads(
        Json.obj(
          "metadata" -> metadata,
          "payload"  -> payload
        )
      )
      val expected = DetailsRequest(metadata, Some(payload))
      actual mustBe JsSuccess(expected)
  }

}
