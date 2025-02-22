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
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

sealed abstract class Channel(val channel: String) extends Product with Serializable

object Channel {
  case object Api extends Channel("api")
  case object Web extends Channel("web")

  val values = Seq(Api, Web)

  def findByChannel(channel: String): Option[Channel] =
    values.find(_.channel == channel)

  implicit val channelWrites: Writes[Channel] = new Writes[Channel] {
    override def writes(channel: Channel): JsValue = Json.toJson(channel.channel)
  }

  implicit val channelReads: Reads[Channel] = Reads {
    case JsString(value) => findByChannel(value).map(JsSuccess(_)).getOrElse(JsError("Invalid Channel name"))
    case _               => JsError("Invalid Channel name")
  }

  def getChannel(clientId: Option[ClientId]): Option[Channel] =
    if (clientId.isDefined) {
      Some(Api)
    } else {
      Some(Web)
    }
}
