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

import play.api.libs.json.JsError
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

sealed trait Channel

object Channel {
  final case object API extends Channel
  final case object WEB extends Channel

  val values = Seq(API, WEB)

  implicit val channelWrites = new Writes[Channel] {
    override def writes(channel: Channel) = Json.toJson(channel.toString)
  }

  implicit val channelReads: Reads[Channel] = Reads {
    case JsString("API") => JsSuccess(API)
    case JsString("WEB") => JsSuccess(WEB)
    case _               => JsError("Invalid Channel name")
  }

  def getChannel(clientId: Option[ClientId]): Option[Channel] =
    if (clientId.isDefined) {
      Some(API)
    } else {
      Some(WEB)
    }
}
