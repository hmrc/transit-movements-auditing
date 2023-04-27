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

import akka.stream.alpakka.xml.ParseEvent
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.transitmovementsauditing.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.models.errors.ParseError
import uk.gov.hmrc.transitmovementsauditing.services.XmlParsers.ParseResult
import uk.gov.hmrc.transitmovementsauditing.services.XmlParsers.concatKeyValue

import scala.concurrent.Future
import scala.xml.NodeSeq

class XmlParserSpec
    extends AnyFreeSpec
    with TestActorSystem
    with Matchers
    with StreamTestHelpers
    with BeforeAndAfterEach
    with ScalaFutures
    with ScalaCheckDrivenPropertyChecks {

  "Movement Reference Number parser" - {

    val cc007c: NodeSeq =
      <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
        <messageSender>message-sender-1</messageSender>
        <messageRecipient>message-recipient-1</messageRecipient>
        <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
        <messageIdentification>token</messageIdentification>
        <messageType>CD975C</messageType>
        <!--Optional:-->
        <correlationIdentifier>token</correlationIdentifier>
        <TransitOperation>
          <MRN>MRN-1</MRN>
          <arrivalNotificationDateAndTime>2014-06-09T16:15:04+01:00</arrivalNotificationDateAndTime>
          <simplifiedProcedure>1</simplifiedProcedure>
          <incidentFlag>1</incidentFlag>
        </TransitOperation>
        <!--0 to 9 repetitions:-->
        <Authorisation>
          <sequenceNumber>token</sequenceNumber>
          <type>token</type>
          <referenceNumber>string</referenceNumber>
        </Authorisation>
        <CustomsOfficeOfDestinationActual>
          <referenceNumber>Newcastle-airport-1</referenceNumber>
        </CustomsOfficeOfDestinationActual>
        <TraderAtDestination>
          <identificationNumber>string</identificationNumber>
          <!--Optional:-->
          <communicationLanguageAtDestination>token</communicationLanguageAtDestination>
        </TraderAtDestination>
        <Consignment>
          <LocationOfGoods>
            <typeOfLocation>token</typeOfLocation>
            <qualifierOfIdentification>token</qualifierOfIdentification>
            <!--Optional:-->
            <authorisationNumber>string</authorisationNumber>
            <!--Optional:-->
            <additionalIdentifier>stri</additionalIdentifier>
            <!--Optional:-->
            <UNLocode>token</UNLocode>
            <!--Optional:-->
            <CustomsOffice>
              <referenceNumber>stringst</referenceNumber>
            </CustomsOffice>
            <!--Optional:-->
            <GNSS>
              <latitude>string</latitude>
              <longitude>string</longitude>
            </GNSS>
            <!--Optional:-->
            <EconomicOperator>
              <identificationNumber>string</identificationNumber>
            </EconomicOperator>
            <!--Optional:-->
            <Address>
              <streetAndNumber>string</streetAndNumber>
              <!--Optional:-->
              <postcode>string</postcode>
              <city>string</city>
              <country>st</country>
            </Address>
            <!--Optional:-->
            <PostcodeAddress>
              <!--Optional:-->
              <houseNumber>string</houseNumber>
              <postcode>string</postcode>
              <country>st</country>
            </PostcodeAddress>
            <!--Optional:-->
            <ContactPerson>
              <name>string</name>
              <phoneNumber>token</phoneNumber>
              <!--Optional:-->
              <eMailAddress>string</eMailAddress>
            </ContactPerson>
          </LocationOfGoods>
          <!--0 to 9 repetitions:-->
          <Incident>
            <sequenceNumber>token</sequenceNumber>
            <code>token</code>
            <text>string</text>
            <!--Optional:-->
            <Endorsement>
              <date>2013-05-22+01:00</date>
              <authority>string</authority>
              <place>string</place>
              <country>st</country>
            </Endorsement>
            <Location>
              <qualifierOfIdentification>token</qualifierOfIdentification>
              <!--Optional:-->
              <UNLocode>token</UNLocode>
              <country>st</country>
              <!--Optional:-->
              <GNSS>
                <latitude>string</latitude>
                <longitude>string</longitude>
              </GNSS>
              <!--Optional:-->
              <Address>
                <streetAndNumber>string</streetAndNumber>
                <!--Optional:-->
                <postcode>string</postcode>
                <city>string</city>
              </Address>
            </Location>
            <!--0 to 9999 repetitions:-->
            <TransportEquipment>
              <sequenceNumber>token</sequenceNumber>
              <!--Optional:-->
              <containerIdentificationNumber>string</containerIdentificationNumber>
              <!--Optional:-->
              <numberOfSeals>100</numberOfSeals>
              <!--0 to 99 repetitions:-->
              <Seal>
                <sequenceNumber>token</sequenceNumber>
                <identifier>string</identifier>
              </Seal>
              <!--0 to 9999 repetitions:-->
              <GoodsReference>
                <sequenceNumber>token</sequenceNumber>
                <declarationGoodsItemNumber>100</declarationGoodsItemNumber>
              </GoodsReference>
            </TransportEquipment>
            <!--Optional:-->
            <Transhipment>
              <containerIndicator>0</containerIndicator>
              <TransportMeans>
                <typeOfIdentification>token</typeOfIdentification>
                <identificationNumber>string</identificationNumber>
                <nationality>st</nationality>
              </TransportMeans>
            </Transhipment>
          </Incident>
        </Consignment>
      </ncts:CC007C>

    val cc015c: NodeSeq = <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
          <messageSender>message-sender-2</messageSender>
          <messageRecipient>token</messageRecipient>
          <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
          <messageIdentification>token</messageIdentification>
          <messageType>CD975C</messageType>
          <!--Optional:-->
          <correlationIdentifier>token</correlationIdentifier>
          <TransitOperation>
            <LRN>LRN-2</LRN>
            <declarationType>token</declarationType>
            <additionalDeclarationType>token</additionalDeclarationType>
            <!--Optional:-->
            <TIRCarnetNumber>string</TIRCarnetNumber>
            <!--Optional:-->
            <presentationOfTheGoodsDateAndTime>2014-06-09T16:15:04+01:00</presentationOfTheGoodsDateAndTime>
            <security>token</security>
            <reducedDatasetIndicator>1</reducedDatasetIndicator>
            <!--Optional:-->
            <specificCircumstanceIndicator>token</specificCircumstanceIndicator>
            <!--Optional:-->
            <communicationLanguageAtDeparture>st</communicationLanguageAtDeparture>
            <bindingItinerary>1</bindingItinerary>
            <!--Optional:-->
            <limitDate>2013-05-22+01:00</limitDate>
          </TransitOperation>
          <!--0 to 9 repetitions:-->
          <Authorisation>
            <sequenceNumber>token</sequenceNumber>
            <type>token</type>
            <referenceNumber>string</referenceNumber>
          </Authorisation>
          <CustomsOfficeOfDeparture>
            <referenceNumber>Newcastle-airport-2</referenceNumber>
          </CustomsOfficeOfDeparture>
          <CustomsOfficeOfDestinationDeclared>
            <referenceNumber>Newcastle-train-station-2</referenceNumber>
          </CustomsOfficeOfDestinationDeclared>
          <!--0 to 9 repetitions:-->
          <CustomsOfficeOfTransitDeclared>
            <sequenceNumber>token</sequenceNumber>
            <referenceNumber>stringst</referenceNumber>
            <!--Optional:-->
            <arrivalDateAndTimeEstimated>2002-11-05T08:01:03+00:00</arrivalDateAndTimeEstimated>
          </CustomsOfficeOfTransitDeclared>
          <!--0 to 9 repetitions:-->
          <CustomsOfficeOfExitForTransitDeclared>
            <sequenceNumber>token</sequenceNumber>
            <referenceNumber>stringst</referenceNumber>
          </CustomsOfficeOfExitForTransitDeclared>
          <HolderOfTheTransitProcedure>
            <!--Optional:-->
            <identificationNumber>string</identificationNumber>
            <!--Optional:-->
            <TIRHolderIdentificationNumber>string</TIRHolderIdentificationNumber>
            <!--Optional:-->
            <name>string</name>
            <!--Optional:-->
            <Address>
              <streetAndNumber>string</streetAndNumber>
              <!--Optional:-->
              <postcode>string</postcode>
              <city>string</city>
              <country>st</country>
            </Address>
            <!--Optional:-->
            <ContactPerson>
              <name>string</name>
              <phoneNumber>token</phoneNumber>
              <!--Optional:-->
              <eMailAddress>string</eMailAddress>
            </ContactPerson>
          </HolderOfTheTransitProcedure>
          <!--Optional:-->
          <Representative>
            <identificationNumber>string</identificationNumber>
            <status>token</status>
            <!--Optional:-->
            <ContactPerson>
              <name>string</name>
              <phoneNumber>token</phoneNumber>
              <!--Optional:-->
              <eMailAddress>string</eMailAddress>
            </ContactPerson>
          </Representative>
          <!--1 to 9 repetitions:-->
          <Guarantee>
            <sequenceNumber>token</sequenceNumber>
            <guaranteeType>s</guaranteeType>
            <!--Optional:-->
            <otherGuaranteeReference>string</otherGuaranteeReference>
            <!--0 to 99 repetitions:-->
            <GuaranteeReference>
              <sequenceNumber>token</sequenceNumber>
              <!--Optional:-->
              <GRN>string</GRN>
              <!--Optional:-->
              <accessCode>stri</accessCode>
              <!--Optional:-->
              <amountToBeCovered>1000.000000000000</amountToBeCovered>
              <!--Optional:-->
              <currency>token</currency>
            </GuaranteeReference>
          </Guarantee>
          <Consignment>
            <!--Optional:-->
            <countryOfDispatch>st</countryOfDispatch>
            <!--Optional:-->
            <countryOfDestination>token</countryOfDestination>
            <!--Optional:-->
            <containerIndicator>1</containerIndicator>
            <!--Optional:-->
            <inlandModeOfTransport>token</inlandModeOfTransport>
            <!--Optional:-->
            <modeOfTransportAtTheBorder>token</modeOfTransportAtTheBorder>
            <grossMass>1000.000000000000</grossMass>
            <!--Optional:-->
            <referenceNumberUCR>string</referenceNumberUCR>
            <!--Optional:-->
            <Carrier>
              <identificationNumber>string</identificationNumber>
              <!--Optional:-->
              <ContactPerson>
                <name>string</name>
                <phoneNumber>token</phoneNumber>
                <!--Optional:-->
                <eMailAddress>string</eMailAddress>
              </ContactPerson>
            </Carrier>
            <!--Optional:-->
            <Consignor>
              <!--Optional:-->
              <identificationNumber>string</identificationNumber>
              <!--Optional:-->
              <name>string</name>
              <!--Optional:-->
              <Address>
                <streetAndNumber>string</streetAndNumber>
                <!--Optional:-->
                <postcode>string</postcode>
                <city>string</city>
                <country>st</country>
              </Address>
              <!--Optional:-->
              <ContactPerson>
                <name>string</name>
                <phoneNumber>token</phoneNumber>
                <!--Optional:-->
                <eMailAddress>string</eMailAddress>
              </ContactPerson>
            </Consignor>
            <!--Optional:-->
            <Consignee>
              <!--Optional:-->
              <identificationNumber>string</identificationNumber>
              <!--Optional:-->
              <name>string</name>
              <!--Optional:-->
              <Address>
                <streetAndNumber>string</streetAndNumber>
                <!--Optional:-->
                <postcode>string</postcode>
                <city>string</city>
                <country>st</country>
              </Address>
            </Consignee>
            <!--0 to 99 repetitions:-->
            <AdditionalSupplyChainActor>
              <sequenceNumber>token</sequenceNumber>
              <role>token</role>
              <identificationNumber>string</identificationNumber>
            </AdditionalSupplyChainActor>
            <!--0 to 9999 repetitions:-->
            <TransportEquipment>
              <sequenceNumber>token</sequenceNumber>
              <!--Optional:-->
              <containerIdentificationNumber>string</containerIdentificationNumber>
              <numberOfSeals>100</numberOfSeals>
              <!--0 to 99 repetitions:-->
              <Seal>
                <sequenceNumber>token</sequenceNumber>
                <identifier>string</identifier>
              </Seal>
              <!--0 to 9999 repetitions:-->
              <GoodsReference>
                <sequenceNumber>token</sequenceNumber>
                <declarationGoodsItemNumber>100</declarationGoodsItemNumber>
              </GoodsReference>
            </TransportEquipment>
            <!--Optional:-->
            <LocationOfGoods>
              <typeOfLocation>token</typeOfLocation>
              <qualifierOfIdentification>token</qualifierOfIdentification>
              <!--Optional:-->
              <authorisationNumber>string</authorisationNumber>
              <!--Optional:-->
              <additionalIdentifier>stri</additionalIdentifier>
              <!--Optional:-->
              <UNLocode>token</UNLocode>
              <!--Optional:-->
              <CustomsOffice>
                <referenceNumber>stringst</referenceNumber>
              </CustomsOffice>
              <!--Optional:-->
              <GNSS>
                <latitude>string</latitude>
                <longitude>string</longitude>
              </GNSS>
              <!--Optional:-->
              <EconomicOperator>
                <identificationNumber>string</identificationNumber>
              </EconomicOperator>
              <!--Optional:-->
              <Address>
                <streetAndNumber>string</streetAndNumber>
                <!--Optional:-->
                <postcode>string</postcode>
                <city>string</city>
                <country>st</country>
              </Address>
              <!--Optional:-->
              <PostcodeAddress>
                <!--Optional:-->
                <houseNumber>string</houseNumber>
                <postcode>string</postcode>
                <country>st</country>
              </PostcodeAddress>
              <!--Optional:-->
              <ContactPerson>
                <name>string</name>
                <phoneNumber>token</phoneNumber>
                <!--Optional:-->
                <eMailAddress>string</eMailAddress>
              </ContactPerson>
            </LocationOfGoods>
            <!--0 to 999 repetitions:-->
            <DepartureTransportMeans>
              <sequenceNumber>token</sequenceNumber>
              <!--Optional:-->
              <typeOfIdentification>token</typeOfIdentification>
              <!--Optional:-->
              <identificationNumber>string</identificationNumber>
              <!--Optional:-->
              <nationality>st</nationality>
            </DepartureTransportMeans>
            <!--0 to 99 repetitions:-->
            <CountryOfRoutingOfConsignment>
              <sequenceNumber>token</sequenceNumber>
              <country>st</country>
            </CountryOfRoutingOfConsignment>
            <!--0 to 9 repetitions:-->
            <ActiveBorderTransportMeans>
              <sequenceNumber>token</sequenceNumber>
              <!--Optional:-->
              <customsOfficeAtBorderReferenceNumber>token</customsOfficeAtBorderReferenceNumber>
              <!--Optional:-->
              <typeOfIdentification>token</typeOfIdentification>
              <!--Optional:-->
              <identificationNumber>string</identificationNumber>
              <!--Optional:-->
              <nationality>st</nationality>
              <!--Optional:-->
              <conveyanceReferenceNumber>string</conveyanceReferenceNumber>
            </ActiveBorderTransportMeans>
            <!--Optional:-->
            <PlaceOfLoading>
              <!--Optional:-->
              <UNLocode>token</UNLocode>
              <!--Optional:-->
              <country>st</country>
              <!--Optional:-->
              <location>string</location>
            </PlaceOfLoading>
            <!--Optional:-->
            <PlaceOfUnloading>
              <!--Optional:-->
              <UNLocode>token</UNLocode>
              <!--Optional:-->
              <country>st</country>
              <!--Optional:-->
              <location>string</location>
            </PlaceOfUnloading>
            <!--0 to 9999 repetitions:-->
            <PreviousDocument>
              <sequenceNumber>token</sequenceNumber>
              <type>token</type>
              <referenceNumber>string</referenceNumber>
              <!--Optional:-->
              <complementOfInformation>string</complementOfInformation>
            </PreviousDocument>
            <!--0 to 99 repetitions:-->
            <SupportingDocument>
              <sequenceNumber>token</sequenceNumber>
              <type>token</type>
              <referenceNumber>string</referenceNumber>
              <!--Optional:-->
              <documentLineItemNumber>100</documentLineItemNumber>
              <!--Optional:-->
              <complementOfInformation>string</complementOfInformation>
            </SupportingDocument>
            <!--0 to 99 repetitions:-->
            <TransportDocument>
              <sequenceNumber>token</sequenceNumber>
              <type>token</type>
              <referenceNumber>string</referenceNumber>
            </TransportDocument>
            <!--0 to 99 repetitions:-->
            <AdditionalReference>
              <sequenceNumber>token</sequenceNumber>
              <type>token</type>
              <!--Optional:-->
              <referenceNumber>string</referenceNumber>
            </AdditionalReference>
            <!--0 to 99 repetitions:-->
            <AdditionalInformation>
              <sequenceNumber>token</sequenceNumber>
              <code>token</code>
              <!--Optional:-->
              <text>string</text>
            </AdditionalInformation>
            <!--Optional:-->
            <TransportCharges>
              <methodOfPayment>s</methodOfPayment>
            </TransportCharges>
            <!--1 to 99 repetitions:-->
            <HouseConsignment>
              <sequenceNumber>token</sequenceNumber>
              <!--Optional:-->
              <countryOfDispatch>st</countryOfDispatch>
              <grossMass>1000.000000000000</grossMass>
              <!--Optional:-->
              <referenceNumberUCR>string</referenceNumberUCR>
              <!--Optional:-->
              <Consignor>
                <!--Optional:-->
                <identificationNumber>string</identificationNumber>
                <!--Optional:-->
                <name>string</name>
                <!--Optional:-->
                <Address>
                  <streetAndNumber>string</streetAndNumber>
                  <!--Optional:-->
                  <postcode>string</postcode>
                  <city>string</city>
                  <country>st</country>
                </Address>
                <!--Optional:-->
                <ContactPerson>
                  <name>string</name>
                  <phoneNumber>token</phoneNumber>
                  <!--Optional:-->
                  <eMailAddress>string</eMailAddress>
                </ContactPerson>
              </Consignor>
              <!--Optional:-->
              <Consignee>
                <!--Optional:-->
                <identificationNumber>string</identificationNumber>
                <!--Optional:-->
                <name>string</name>
                <!--Optional:-->
                <Address>
                  <streetAndNumber>string</streetAndNumber>
                  <!--Optional:-->
                  <postcode>string</postcode>
                  <city>string</city>
                  <country>st</country>
                </Address>
              </Consignee>
              <!--0 to 99 repetitions:-->
              <AdditionalSupplyChainActor>
                <sequenceNumber>token</sequenceNumber>
                <role>token</role>
                <identificationNumber>string</identificationNumber>
              </AdditionalSupplyChainActor>
              <!--0 to 999 repetitions:-->
              <DepartureTransportMeans>
                <sequenceNumber>token</sequenceNumber>
                <typeOfIdentification>token</typeOfIdentification>
                <identificationNumber>string</identificationNumber>
                <nationality>st</nationality>
              </DepartureTransportMeans>
              <!--0 to 99 repetitions:-->
              <PreviousDocument>
                <sequenceNumber>token</sequenceNumber>
                <type>token</type>
                <referenceNumber>string</referenceNumber>
                <!--Optional:-->
                <complementOfInformation>string</complementOfInformation>
              </PreviousDocument>
              <!--0 to 99 repetitions:-->
              <SupportingDocument>
                <sequenceNumber>token</sequenceNumber>
                <type>token</type>
                <referenceNumber>string</referenceNumber>
                <!--Optional:-->
                <documentLineItemNumber>100</documentLineItemNumber>
                <!--Optional:-->
                <complementOfInformation>string</complementOfInformation>
              </SupportingDocument>
              <!--0 to 99 repetitions:-->
              <TransportDocument>
                <sequenceNumber>token</sequenceNumber>
                <type>token</type>
                <referenceNumber>string</referenceNumber>
              </TransportDocument>
              <!--0 to 99 repetitions:-->
              <AdditionalReference>
                <sequenceNumber>token</sequenceNumber>
                <type>token</type>
                <!--Optional:-->
                <referenceNumber>string</referenceNumber>
              </AdditionalReference>
              <!--0 to 99 repetitions:-->
              <AdditionalInformation>
                <sequenceNumber>token</sequenceNumber>
                <code>token</code>
                <!--Optional:-->
                <text>string</text>
              </AdditionalInformation>
              <!--Optional:-->
              <TransportCharges>
                <methodOfPayment>s</methodOfPayment>
              </TransportCharges>
              <!--1 to 999 repetitions:-->
              <ConsignmentItem>
                <goodsItemNumber>token</goodsItemNumber>
                <declarationGoodsItemNumber>100</declarationGoodsItemNumber>
                <!--Optional:-->
                <declarationType>declaration-type-2</declarationType>
                <!--Optional:-->
                <countryOfDispatch>st</countryOfDispatch>
                <!--Optional:-->
                <countryOfDestination>token</countryOfDestination>
                <!--Optional:-->
                <referenceNumberUCR>string</referenceNumberUCR>
                <!--Optional:-->
                <Consignee>
                  <!--Optional:-->
                  <identificationNumber>string</identificationNumber>
                  <!--Optional:-->
                  <name>string</name>
                  <!--Optional:-->
                  <Address>
                    <streetAndNumber>string</streetAndNumber>
                    <!--Optional:-->
                    <postcode>string</postcode>
                    <city>string</city>
                    <country>st</country>
                  </Address>
                </Consignee>
                <!--0 to 99 repetitions:-->
                <AdditionalSupplyChainActor>
                  <sequenceNumber>token</sequenceNumber>
                  <role>token</role>
                  <identificationNumber>string</identificationNumber>
                </AdditionalSupplyChainActor>
                <Commodity>
                  <descriptionOfGoods>string</descriptionOfGoods>
                  <!--Optional:-->
                  <cusCode>token</cusCode>
                  <!--Optional:-->
                  <CommodityCode>
                    <harmonizedSystemSubHeadingCode>token</harmonizedSystemSubHeadingCode>
                    <!--Optional:-->
                    <combinedNomenclatureCode>st</combinedNomenclatureCode>
                  </CommodityCode>
                  <!--0 to 99 repetitions:-->
                  <DangerousGoods>
                    <sequenceNumber>token</sequenceNumber>
                    <UNNumber>token</UNNumber>
                  </DangerousGoods>
                  <!--Optional:-->
                  <GoodsMeasure>
                    <!--Optional:-->
                    <grossMass>1000.000000000000</grossMass>
                    <!--Optional:-->
                    <netMass>1000.000000000000</netMass>
                    <!--Optional:-->
                    <supplementaryUnits>1000.000000000000</supplementaryUnits>
                  </GoodsMeasure>
                </Commodity>
                <!--1 to 99 repetitions:-->
                <Packaging>
                  <sequenceNumber>token</sequenceNumber>
                  <typeOfPackages>token</typeOfPackages>
                  <!--Optional:-->
                  <numberOfPackages>100</numberOfPackages>
                  <!--Optional:-->
                  <shippingMarks>string</shippingMarks>
                </Packaging>
                <!--0 to 99 repetitions:-->
                <PreviousDocument>
                  <sequenceNumber>token</sequenceNumber>
                  <type>token</type>
                  <referenceNumber>string</referenceNumber>
                  <!--Optional:-->
                  <goodsItemNumber>100</goodsItemNumber>
                  <!--Optional:-->
                  <typeOfPackages>token</typeOfPackages>
                  <!--Optional:-->
                  <numberOfPackages>100</numberOfPackages>
                  <!--Optional:-->
                  <measurementUnitAndQualifier>token</measurementUnitAndQualifier>
                  <!--Optional:-->
                  <quantity>1000.000000000000</quantity>
                  <!--Optional:-->
                  <complementOfInformation>string</complementOfInformation>
                </PreviousDocument>
                <!--0 to 99 repetitions:-->
                <SupportingDocument>
                  <sequenceNumber>token</sequenceNumber>
                  <type>token</type>
                  <referenceNumber>string</referenceNumber>
                  <!--Optional:-->
                  <documentLineItemNumber>100</documentLineItemNumber>
                  <!--Optional:-->
                  <complementOfInformation>string</complementOfInformation>
                </SupportingDocument>
                <!--0 to 99 repetitions:-->
                <TransportDocument>
                  <sequenceNumber>token</sequenceNumber>
                  <type>token</type>
                  <referenceNumber>string</referenceNumber>
                </TransportDocument>
                <!--0 to 99 repetitions:-->
                <AdditionalReference>
                  <sequenceNumber>token</sequenceNumber>
                  <type>token</type>
                  <!--Optional:-->
                  <referenceNumber>string</referenceNumber>
                </AdditionalReference>
                <!--0 to 99 repetitions:-->
                <AdditionalInformation>
                  <sequenceNumber>token</sequenceNumber>
                  <code>token</code>
                  <!--Optional:-->
                  <text>string</text>
                </AdditionalInformation>
                <!--Optional:-->
                <TransportCharges>
                  <methodOfPayment>s</methodOfPayment>
                </TransportCharges>
              </ConsignmentItem>
            </HouseConsignment>
          </Consignment>
        </ncts:CC015C>

    val elementPaths: Map[String, Map[String, Seq[String]]] = Map(
      "CC007C" -> Map(
        "messageSender"                    -> ("CC007C" :: "messageSender" :: Nil),
        "MRN"                              -> ("CC007C" :: "TransitOperation" :: "MRN" :: Nil),
        "CustomsOfficeOfDestinationActual" -> ("CC007C" :: "CustomsOfficeOfDestinationActual" :: "referenceNumber" :: Nil)
      ),
      "CC015C" -> Map(
        "messageSender"                      -> ("CC015C" :: "messageSender" :: Nil),
        "LRN"                                -> ("CC015C" :: "TransitOperation" :: "LRN" :: Nil),
        "declarationType"                    -> ("CC015C" :: "Consignment" :: "HouseConsignment" :: "ConsignmentItem" :: "declarationType" :: Nil),
        "CustomsOfficeOfDeparture"           -> ("CC015C" :: "CustomsOfficeOfDeparture" :: "referenceNumber" :: Nil),
        "CustomsOfficeOfDestinationDeclared" -> ("CC015C" :: "CustomsOfficeOfDestinationDeclared" :: "referenceNumber" :: Nil)
      )
    )

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    "when a valid CC015C message is provided extract messageSender element" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("CC015C")
      val result                        = stream.via(XmlParsers.extractElement("messageSender", paths("messageSender"))).runWith(Sink.head)
      whenReady(result) {
        value =>
          value mustBe Right("(messageSender,message-sender-2)")
      }
    }

    "when a valid CC015C message is provided extract LRN element" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("CC015C")
      val result                        = stream.via(XmlParsers.extractElement("LRN", paths("LRN"))).runWith(Sink.head)
      whenReady(result) {
        lrn =>
          lrn mustBe Right("(LRN,LRN-2)")
      }
    }

    "when a valid CC015C message is provided extract declaration type element" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("CC015C")
      val result                        = stream.via(XmlParsers.extractElement("declarationType", paths("declarationType"))).runWith(Sink.head)
      whenReady(result) {
        declaration =>
          declaration mustBe Right("(declarationType,declaration-type-2)")
      }
    }

    "when a valid CC015C message is provided extract CustomsOfficeOfDeparture element" in {
      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("CC015C")

      val result =
        stream.via(XmlParsers.extractElement("CustomsOfficeOfDeparture", paths("CustomsOfficeOfDeparture"))).runWith(Sink.head)
      whenReady(result) {
        office =>
          office mustBe Right("(CustomsOfficeOfDeparture,Newcastle-airport-2)")
      }
    }

    "when a valid CC015C message is provided extract CustomsOfficeOfDestinationDeclared element" in {
      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("CC015C")

      val result =
        stream.via(XmlParsers.extractElement("CustomsOfficeOfDestinationDeclared", paths("CustomsOfficeOfDestinationDeclared"))).runWith(Sink.head)
      whenReady(result) {
        office =>
          office mustBe Right("(CustomsOfficeOfDestinationDeclared,Newcastle-train-station-2)")
      }
    }

    "when a valid CC007C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc007c)

      implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

      val additionalFields: Iterable[concurrent.Future[ParseResult[String]]] = elementPaths("CC007C").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right("(messageSender,message-sender-1)"),
            Right("(MRN,MRN-1)"),
            Right("(CustomsOfficeOfDestinationActual,Newcastle-airport-1)")
          )
      }

    }

    "when a valid CC015C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[String]]] = elementPaths("CC015C").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[String]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right("(LRN,LRN-2)"),
            Right("(declarationType,declaration-type-2)"),
            Right("(messageSender,message-sender-2)"),
            Right("(CustomsOfficeOfDestinationDeclared,Newcastle-train-station-2)"),
            Right("(CustomsOfficeOfDeparture,Newcastle-airport-2)")
          )
      }
    }

  }

}
