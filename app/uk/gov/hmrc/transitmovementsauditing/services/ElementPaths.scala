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

package uk.gov.hmrc.transitmovementsauditing.services

trait ElementPaths {

  val elementPaths: Map[String, Map[String, Seq[String]]] = Map( //TODO: Check all
    "IE007" -> Map(
      "messageSender"                    -> ("CC007C" :: "messageSender" :: Nil),
      "MRN"                              -> ("CC007C" :: "TransitOperation" :: "MRN" :: Nil),
      "CustomsOfficeOfDestinationActual" -> ("CC007C" :: "CustomsOfficeOfDestinationActual" :: "referenceNumber" :: Nil)
    ),
    "IE013" -> Map(
      "messageSender"                      -> ("CC013C" :: "messageSender" :: Nil),
      "LRN"                                -> ("CC013C" :: "TransitOperation" :: "LRN" :: Nil),
      "declarationType"                    -> ("CC013C" :: "TransitOperation" :: "declarationType" :: Nil),
      "CustomsOfficeOfDeparture"           -> ("CC013C" :: "CustomsOfficeOfDeparture" :: "referenceNumber" :: Nil),
      "CustomsOfficeOfDestinationDeclared" -> ("CC013C" :: "CustomsOfficeOfDestinationDeclared" :: "referenceNumber" :: Nil)
    ),
    "IE014" -> Map(
      "messageSender"            -> ("CC014C" :: "messageSender" :: Nil),
      "LRN"                      -> ("CC014C" :: "TransitOperation" :: "LRN" :: Nil),
      "MRN"                      -> ("CC014C" :: "TransitOperation" :: "MRN" :: Nil),
      "CustomsOfficeOfDeparture" -> ("CC014C" :: "CustomsOfficeOfDeparture" :: "referenceNumber" :: Nil)
    ),
    "IE015" -> Map(
      "messageSender"                      -> ("CC015C" :: "messageSender" :: Nil),
      "LRN"                                -> ("CC015C" :: "TransitOperation" :: "LRN" :: Nil),
      "declarationType"                    -> ("CC015C" :: "TransitOperation" :: "declarationType" :: Nil),
      "CustomsOfficeOfDeparture"           -> ("CC015C" :: "CustomsOfficeOfDeparture" :: "referenceNumber" :: Nil),
      "CustomsOfficeOfDestinationDeclared" -> ("CC015C" :: "CustomsOfficeOfDestinationDeclared" :: "referenceNumber" :: Nil)
    ),
    "IE044" -> Map(
      "messageSender" -> ("CC044C" :: "messageSender" :: Nil)
    ),
    "IE170" -> Map(
      "messageSender"            -> ("CC170C" :: "messageSender" :: Nil),
      "LRN"                      -> ("CC170C" :: "TransitOperation" :: "LRN" :: Nil),
      "CustomsOfficeOfDeparture" -> ("CC170C" :: "CustomsOfficeOfDeparture" :: "referenceNumber" :: Nil)
    ),
    "IE141" -> Map(
      "messageSender"                    -> ("CC141C" :: "messageSender" :: Nil),
      "MRN"                              -> ("CC141C" :: "TransitOperation" :: "MRN" :: Nil),
      "CustomsOfficeOfDestinationActual" -> ("CC141C" :: "CustomsOfficeOfDestinationActual" :: "referenceNumber" :: Nil)
    )
  )
}
