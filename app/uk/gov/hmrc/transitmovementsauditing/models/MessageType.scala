/*
 * Copyright 2022 HM Revenue & Customs
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

sealed abstract class MessageType(val messageCode: String) extends Product with Serializable

object MessageType {
  case object IE004 extends MessageType("IE004")
  case object IE007 extends MessageType("IE007")
  case object IE009 extends MessageType("IE009")
  case object IE013 extends MessageType("IE013")
  case object IE014 extends MessageType("IE014")
  case object IE015 extends MessageType("IE015")
  case object IE019 extends MessageType("IE019")
  case object IE022 extends MessageType("IE022")
  case object IE025 extends MessageType("IE025")
  case object IE028 extends MessageType("IE028")
  case object IE029 extends MessageType("IE029")
  case object IE035 extends MessageType("IE035")
  case object IE043 extends MessageType("IE043")
  case object IE044 extends MessageType("IE044")
  case object IE141 extends MessageType("IE141")
  case object IE045 extends MessageType("IE045")
  case object IE051 extends MessageType("IE051")
  case object IE054 extends MessageType("IE054")
  case object IE055 extends MessageType("IE055")
  case object IE056 extends MessageType("IE056")
  case object IE057 extends MessageType("IE057")
  case object IE060 extends MessageType("IE060")
  case object IE170 extends MessageType("IE170")
  case object IE182 extends MessageType("IE182")
  case object IE928 extends MessageType("IE928")
  case object IE140 extends MessageType("IE140")

  val values: Seq[MessageType] = Seq(
    IE004,
    IE007,
    IE009,
    IE013,
    IE014,
    IE015,
    IE019,
    IE022,
    IE025,
    IE028,
    IE029,
    IE035,
    IE043,
    IE044,
    IE045,
    IE051,
    IE054,
    IE055,
    IE056,
    IE057,
    IE060,
    IE170,
    IE182,
    IE928,
    IE140
  )
}
