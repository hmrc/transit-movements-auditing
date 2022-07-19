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

sealed abstract class MessageType(val messageCode: String)

object MessageType {
  case object CC004C extends MessageType("CC004C")
  case object CC007C extends MessageType("CC007C")
  case object CC009C extends MessageType("CC009C")
  case object CC013C extends MessageType("CC013C")
  case object CC014C extends MessageType("CC014C")
  case object CC015C extends MessageType("CC015C")
  case object CC019C extends MessageType("CC019C")
  case object CC022C extends MessageType("CC022C")
  case object CC025C extends MessageType("CC025C")
  case object CC028C extends MessageType("CC028C")
  case object CC029C extends MessageType("CC029C")
  case object CC035C extends MessageType("CC035C")
  case object CC043C extends MessageType("CC043C")
  case object CC044C extends MessageType("CC044C")
  case object CC045C extends MessageType("CC045C")
  case object CC051C extends MessageType("CC051C")
  case object CC054C extends MessageType("CC054C")
  case object CC055C extends MessageType("CC055C")
  case object CC056C extends MessageType("CC056C")
  case object CC057C extends MessageType("CC057C")
  case object CC060C extends MessageType("CC060C")
  case object CC170C extends MessageType("CC170C")
  case object CC182C extends MessageType("CC182C")
  case object CC928C extends MessageType("CC928C")
}
