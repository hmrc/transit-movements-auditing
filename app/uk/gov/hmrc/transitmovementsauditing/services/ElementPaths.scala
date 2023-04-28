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

  def messageSenderFor(message: String)                      = message :: "messageSender" :: Nil
  def messageTypeFor(message: String)                        = message :: "messageType" :: Nil
  def lrnFor(message: String)                                = message :: "TransitOperation" :: "LRN" :: Nil
  def mrnFor(message: String)                                = message :: "TransitOperation" :: "MRN" :: Nil
  def declarationTypeFor(message: String)                    = message :: "TransitOperation" :: "declarationType" :: Nil
  def customsOfficeOfDepartureFor(message: String)           = message :: "CustomsOfficeOfDeparture" :: "referenceNumber" :: Nil
  def customsOfficeOfDestinationDeclaredFor(message: String) = message :: "CustomsOfficeOfDestinationDeclared" :: "referenceNumber" :: Nil
  def customsOfficeOfTransitDeclaredFor(message: String)     = message :: "CustomsOfficeOfTransitDeclared" :: "sequenceNumber" :: Nil
  def customsOfficeOfDestinationActualFor(message: String)   = message :: "CustomsOfficeOfDestinationActual" :: "referenceNumber" :: Nil
  def countryOfDispatchFor(message: String)                  = message :: "Consignment" :: "countryOfDispatch" :: Nil
  def countryOfDestinationFor(message: String)               = message :: "Consignment" :: "countryOfDestination" :: Nil
  def numberOfSealsFor(message: String)                      = message :: "Consignment" :: "TransportEquipment" :: "numberOfSeals" :: Nil
  def numberOfSealsForCC007C                                 = "CC007C" :: "Consignment" :: "Incident" :: "TransportEquipment" :: "numberOfSeals" :: Nil
  def customsOfficeFor(message: String)                      = message :: "Consignment" :: "LocationOfGoods" :: "CustomsOffice" :: "referenceNumber" :: Nil
  def countryOfRoutingOfConsignmentFor(message: String)      = message :: "Consignment" :: "CountryOfRoutingOfConsignment" :: "country" :: Nil
  def previousDocumentFor(message: String)                   = message :: "Consignment" :: "PreviousDocument" :: "referenceNumber" :: Nil
  def grnFor(message: String)                                = message :: "Guarantee" :: "GuaranteeReference" :: "GRN" :: Nil
  def accessCodeFor(message: String)                         = message :: "Guarantee" :: "GuaranteeReference" :: "accessCode" :: Nil
  def economicOperatorFor(message: String)                   = message :: "Consignment" :: "LocationOfGoods" :: "EconomicOperator" :: "identificationNumber" :: Nil

  val elementPaths: Map[String, Map[String, Seq[String]]] = Map( //TODO: Check all
    "IE007" -> Map(
      "messageSender"                    -> messageSenderFor("CC007C"),
      "messageType"                      -> messageTypeFor("CC007C"),
      "MRN"                              -> mrnFor("CC007C"),
      "CustomsOffice"                    -> customsOfficeFor("CC007C"),
      "EconomicOperator"                 -> economicOperatorFor("CC007C"),
      "numberOfSeals"                    -> numberOfSealsForCC007C,
      "CustomsOfficeOfDestinationActual" -> customsOfficeOfDestinationActualFor("CC007C")
    ),
    "IE013" -> Map(
      "messageSender"                      -> messageSenderFor("CC013C"),
      "LRN"                                -> lrnFor("CC013C"),
      "declarationType"                    -> declarationTypeFor("CC013C"),
      "CustomsOfficeOfDeparture"           -> customsOfficeOfDepartureFor("CC013C"),
      "CustomsOfficeOfDestinationDeclared" -> customsOfficeOfDestinationDeclaredFor("CC013C")
    ),
    "IE014" -> Map(
      "messageSender"            -> messageSenderFor("CC014C"),
      "LRN"                      -> lrnFor("CC014C"),
      "MRN"                      -> mrnFor("CC014C"),
      "CustomsOfficeOfDeparture" -> customsOfficeOfDepartureFor("CC014C")
    ),
    "IE015" -> Map(
      "messageSender"                      -> messageSenderFor("CC015C"),
      "messageType"                        -> messageTypeFor("CC015C"),
      "LRN"                                -> lrnFor("CC015C"),
      "declarationType"                    -> declarationTypeFor("CC015C"),
      "CustomsOfficeOfDeparture"           -> customsOfficeOfDepartureFor("CC015C"),
      "CustomsOfficeOfDestinationDeclared" -> customsOfficeOfDestinationDeclaredFor("CC015C"),
      "CustomsOfficeOfTransitDeclared"     -> customsOfficeOfTransitDeclaredFor("CC015C"),
      "countryOfDispatch"                  -> countryOfDispatchFor("CC015C"),
      "countryOfDestination"               -> countryOfDestinationFor("CC015C"),
      "numberOfSeals"                      -> numberOfSealsFor("CC015C"),
      "CustomsOffice"                      -> customsOfficeFor("CC015C"),
      "EconomicOperator"                   -> economicOperatorFor("CC015C"),
      "CountryOfRoutingOfConsignment"      -> countryOfRoutingOfConsignmentFor("CC015C"),
      "PreviousDocument"                   -> previousDocumentFor("CC015C"),
      "GRN"                                -> grnFor("CC015C"),
      "accessCode"                         -> accessCodeFor("CC015C")
    ),
    "IE044" -> Map(
      "messageSender" -> messageSenderFor("CC044C")
    ),
    "IE170" -> Map(
      "messageSender"            -> messageSenderFor("CC170C"),
      "LRN"                      -> lrnFor("CC170C"),
      "CustomsOfficeOfDeparture" -> customsOfficeOfDepartureFor("CC170C")
    ),
    "IE141" -> Map(
      "messageSender"                    -> messageSenderFor("CC141C"),
      "MRN"                              -> mrnFor("CC141C"),
      "CustomsOfficeOfDestinationActual" -> customsOfficeOfDestinationActualFor("CC141C")
    )
  )
}
