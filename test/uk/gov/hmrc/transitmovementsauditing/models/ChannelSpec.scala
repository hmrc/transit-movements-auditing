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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsString
import play.api.libs.json.Json
import uk.gov.hmrc.transitmovementsauditing.models.Channel.API
import uk.gov.hmrc.transitmovementsauditing.models.Channel.WEB

class ChannelSpec extends AnyFlatSpec with Matchers {

  "Channel" should "serialise correctly" in {
    Json.toJson[Channel](Channel.API) should be(JsString("API"))
  }

  "Channel" should "deserialize correctly" in {
    JsString("API").validate[Channel].get should be(Channel.API)
  }

  "getChannel" should "return channel as API if clientId is defined" in {
    Channel.getChannel(Some(ClientId("54321"))) should be(Some(API))
  }

  "getChannel" should "return channel as WEB if clientId is not defined" in {
    Channel.getChannel(None) should be(Some(WEB))
  }

}
