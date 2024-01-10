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

import org.apache.pekko.stream.connectors.xml.ParseEvent
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.transitmovementsauditing.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.services.XmlParsers.ParseResult

import scala.concurrent.Future
import scala.xml.NodeSeq

class XmlParserSpec
    extends AnyFreeSpec
    with ElementPaths
    with TestActorSystem
    with Matchers
    with StreamTestHelpers
    with BeforeAndAfterEach
    with ScalaFutures
    with ScalaCheckDrivenPropertyChecks {

  val cc004c: NodeSeq =
    <ncts:CC004C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
        <messageSender>message-sender-004</messageSender>
        <messageType>CC004C</messageType>
        <TransitOperation>
          <LRN>LRN-004</LRN>
          <MRN>MRN-004</MRN>
        </TransitOperation>
        <CustomsOfficeOfDeparture>
          <referenceNumber>Newcastle-004</referenceNumber>
        </CustomsOfficeOfDeparture>
      </ncts:CC004C>

  val cc007c: NodeSeq =
    <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
        <messageSender>message-sender-1</messageSender>
        <messageType>CC007C</messageType>
        <TransitOperation>
          <MRN>MRN-1</MRN>
        </TransitOperation>
        <CustomsOfficeOfDestinationActual>
          <referenceNumber>Newcastle-airport-1</referenceNumber>
        </CustomsOfficeOfDestinationActual>
        <Consignment>
          <LocationOfGoods>
            <CustomsOffice>
              <referenceNumber>Newcastle-1</referenceNumber>
            </CustomsOffice>
            <EconomicOperator>
              <identificationNumber>GB000001</identificationNumber>
            </EconomicOperator>
          </LocationOfGoods>
          <Incident>
            <TransportEquipment>
              <numberOfSeals>98</numberOfSeals>
            </TransportEquipment>
          </Incident>
        </Consignment>
      </ncts:CC007C>

  val cc009c: NodeSeq = <ncts:CC009C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
          <messageSender>message-sender-009</messageSender>
          <messageType>CC009C</messageType>
          <TransitOperation>
            <LRN>LRN-009</LRN>
            <MRN>MRN-009</MRN>
          </TransitOperation>
          <CustomsOfficeOfDeparture>
            <referenceNumber>Newcastle-009</referenceNumber>
          </CustomsOfficeOfDeparture>
      </ncts:CC009C>

  val cc013c: NodeSeq = <ncts:CC013C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>message-sender-013</messageSender>
      <messageType>CC013C</messageType>
      <TransitOperation>
        <LRN>LRN-013</LRN>
        <MRN>MRN-013</MRN>
        <declarationType>declaration-type-013</declarationType>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>Newcastle-airport-013</referenceNumber>
      </CustomsOfficeOfDeparture>
      <CustomsOfficeOfDestinationDeclared>
        <referenceNumber>Newcastle-train-station-013</referenceNumber>
      </CustomsOfficeOfDestinationDeclared>
      <CustomsOfficeOfTransitDeclared>
        <referenceNumber>Newcastle-port-013</referenceNumber>
      </CustomsOfficeOfTransitDeclared>
      <Guarantee>
        <GuaranteeReference>
          <GRN>guarantee-reference-number-013</GRN>
          <accessCode>guarantee-access-code-013</accessCode>
        </GuaranteeReference>
      </Guarantee>
      <Consignment>
        <countryOfDispatch>UK</countryOfDispatch>
        <countryOfDestination>IT</countryOfDestination>
        <TransportEquipment>
          <numberOfSeals>13</numberOfSeals>
        </TransportEquipment>
        <LocationOfGoods>
          <CustomsOffice>
            <referenceNumber>013</referenceNumber>
          </CustomsOffice>
          <EconomicOperator>
            <identificationNumber>GB-013</identificationNumber>
          </EconomicOperator>
        </LocationOfGoods>
        <CountryOfRoutingOfConsignment>
          <country>ESP</country>
        </CountryOfRoutingOfConsignment>
        <PreviousDocument>
          <referenceNumber>previous-document-013</referenceNumber>
        </PreviousDocument>
        <HouseConsignment>
          <ConsignmentItem>
            <Packaging>
              <numberOfPackages>013</numberOfPackages>
            </Packaging>
          </ConsignmentItem>
        </HouseConsignment>
      </Consignment>
    </ncts:CC013C>

  val cc014c: NodeSeq = <ncts:CC014C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>message-sender-014</messageSender>
      <messageType>CC014C</messageType>
      <TransitOperation>
        <LRN>LRN-014</LRN>
        <MRN>MRN-014</MRN>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>Newcastle-airport-014</referenceNumber>
      </CustomsOfficeOfDeparture>
    </ncts:CC014C>

  val cc015c: NodeSeq = <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
          <messageSender>message-sender-015</messageSender>
          <messageType>CC015C</messageType>
          <TransitOperation>
            <LRN>LRN-015</LRN>
            <declarationType>declaration-type-015</declarationType>
          </TransitOperation>
          <CustomsOfficeOfDeparture>
            <referenceNumber>Newcastle-airport-015</referenceNumber>
          </CustomsOfficeOfDeparture>
          <CustomsOfficeOfDestinationDeclared>
            <referenceNumber>Newcastle-train-station-015</referenceNumber>
          </CustomsOfficeOfDestinationDeclared>
          <CustomsOfficeOfTransitDeclared>
            <sequenceNumber>Newcastle-port-015</sequenceNumber>
          </CustomsOfficeOfTransitDeclared>
      <Guarantee>
        <GuaranteeReference>
          <GRN>guarantee-reference-number-015</GRN>
          <accessCode>guarantee-access-code-015</accessCode>
        </GuaranteeReference>
      </Guarantee>
          <Consignment>
            <countryOfDispatch>UK</countryOfDispatch>
            <countryOfDestination>IT</countryOfDestination>
            <TransportEquipment>
              <numberOfSeals>015</numberOfSeals>
            </TransportEquipment>
            <LocationOfGoods>
              <CustomsOffice>
                <referenceNumber>015</referenceNumber>
              </CustomsOffice>
               <EconomicOperator>
                <identificationNumber>GB015</identificationNumber>
              </EconomicOperator>
            </LocationOfGoods>
            <CountryOfRoutingOfConsignment>
              <country>CH</country>
            </CountryOfRoutingOfConsignment>
            <PreviousDocument>
              <referenceNumber>previous-document-015</referenceNumber>
            </PreviousDocument>
          </Consignment>
        </ncts:CC015C>

  val cc017c: NodeSeq = <ncts:CC017C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-017</messageSender>
    <messageType>CC017C</messageType>
    <TransitOperation>
      <MRN>MRN-017</MRN>
      <declarationType>declaration-type-017</declarationType>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-017</referenceNumber>
    </CustomsOfficeOfDeparture>
    <CustomsOfficeOfDestinationDeclared>
      <referenceNumber>Newcastle-train-station-017</referenceNumber>
    </CustomsOfficeOfDestinationDeclared>
    <CustomsOfficeOfTransitDeclared>
      <referenceNumber>Newcastle-port-017</referenceNumber>
    </CustomsOfficeOfTransitDeclared>
    <CustomsOfficeOfExitForTransitDeclared>
      <referenceNumber>Dover-port-017</referenceNumber>
    </CustomsOfficeOfExitForTransitDeclared>
    <Consignment>
      <countryOfDispatch>UK</countryOfDispatch>
      <countryOfDestination>FR</countryOfDestination>
      <TransportEquipment>
        <numberOfSeals>017</numberOfSeals>
      </TransportEquipment>
      <CountryOfRoutingOfConsignment>
        <country>NL</country>
      </CountryOfRoutingOfConsignment>
      <PreviousDocument>
        <referenceNumber>previous-document-017</referenceNumber>
      </PreviousDocument>
      <HouseConsignment>
        <countryOfDispatch>UK</countryOfDispatch>
        <ConsignmentItem>
          <Packaging>
            <numberOfPackages>017</numberOfPackages>
          </Packaging>
        </ConsignmentItem>
      </HouseConsignment>
    </Consignment>
  </ncts:CC017C>

  val cc019c: NodeSeq = <ncts:CC019C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-019</messageSender>
    <messageType>CC019C</messageType>
    <TransitOperation>
      <MRN>MRN-019</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-019</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC019C>

  val cc022c: NodeSeq = <ncts:CC022C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-022</messageSender>
    <messageType>CC022C</messageType>
    <TransitOperation>
      <MRN>MRN-022</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-022</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC022C>

  val cc023c: NodeSeq = <ncts:CC023C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>message-sender-023</messageSender>
      <messageType>CC023C</messageType>
      <TransitOperation>
        <MRN>MRN-023</MRN>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>Newcastle-airport-023</referenceNumber>
      </CustomsOfficeOfDeparture>
    </ncts:CC023C>

  val cc025c: NodeSeq = <ncts:CC025C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-025</messageSender>
    <messageType>CC025C</messageType>
    <TransitOperation>
      <MRN>MRN-025</MRN>
    </TransitOperation>
    <CustomsOfficeOfDestinationActual>
      <referenceNumber>Newcastle-airport-025</referenceNumber>
    </CustomsOfficeOfDestinationActual>
    <Consignment>
      <HouseConsignment>
        <ConsignmentItem>
          <Packaging>
            <numberOfPackages>025</numberOfPackages>
          </Packaging>
        </ConsignmentItem>
      </HouseConsignment>
    </Consignment>
  </ncts:CC025C>

  val cc028c: NodeSeq = <ncts:CC028C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-028</messageSender>
    <messageType>CC028C</messageType>
    <TransitOperation>
      <LRN>LRN-028</LRN>
      <MRN>MRN-028</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-028</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC028C>

  val cc029c: NodeSeq = <ncts:CC029C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-029</messageSender>
    <messageType>CC029C</messageType>
    <TransitOperation>
      <LRN>LRN-029</LRN>
      <MRN>MRN-029</MRN>
      <declarationType>declaration-type-029</declarationType>
    </TransitOperation>>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-029</referenceNumber>
    </CustomsOfficeOfDeparture>
    <CustomsOfficeOfDestinationDeclared>
      <referenceNumber>Newcastle-train-station-029</referenceNumber>
    </CustomsOfficeOfDestinationDeclared>
    <CustomsOfficeOfTransitDeclared>
      <referenceNumber>Newcastle-port-029</referenceNumber>
    </CustomsOfficeOfTransitDeclared>
    <CustomsOfficeOfExitForTransitDeclared>
      <referenceNumber>Dover-port-029</referenceNumber>
    </CustomsOfficeOfExitForTransitDeclared>
    <Guarantee>
      <GuaranteeReference>
        <GRN>guarantee-reference-number-029</GRN>
        <accessCode>guarantee-access-code-029</accessCode>
      </GuaranteeReference>
    </Guarantee>
    <Consignment>
      <countryOfDispatch>UK</countryOfDispatch>
      <countryOfDestination>IT</countryOfDestination>
      <TransportEquipment>
         <numberOfSeals>029</numberOfSeals>
      </TransportEquipment>
      <LocationOfGoods>
        <CustomsOffice>
          <referenceNumber>029</referenceNumber>
        </CustomsOffice>
        <EconomicOperator>
          <identificationNumber>GB029</identificationNumber>
        </EconomicOperator>
      </LocationOfGoods>
      <CountryOfRoutingOfConsignment>
        <country>DE</country>
      </CountryOfRoutingOfConsignment>
      <PreviousDocument>
        <referenceNumber>previous-document-029</referenceNumber>
      </PreviousDocument>
      <HouseConsignment>
        <ConsignmentItem>
          <Packaging>
            <numberOfPackages>029</numberOfPackages>
          </Packaging>
        </ConsignmentItem>
      </HouseConsignment>
    </Consignment>
  </ncts:CC029C>

  val cc035c: NodeSeq = <ncts:CC035C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-035</messageSender>
    <messageType>CC035C</messageType>
    <TransitOperation>
      <MRN>MRN-035</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-035</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC035C>

  val cc040c: NodeSeq = <ncts:CC040C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-040</messageSender>
    <messageType>CC040C</messageType>
    <TransitOperation>
      <MRN>MRN-040</MRN>
    </TransitOperation>
  </ncts:CC040C>

  val cc042c: NodeSeq = <ncts:CC042C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-042</messageSender>
    <messageType>CC042C</messageType>
    <TransitOperation>
      <MRN>MRN-042</MRN>
    </TransitOperation>
    <CustomsOfficeOfExit>
      <referenceNumber>Dover-port-042</referenceNumber>
    </CustomsOfficeOfExit>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-042</referenceNumber>
    </CustomsOfficeOfDeparture>
    <CustomsOfficeOfDestination>
      <referenceNumber>Paris-042</referenceNumber>
    </CustomsOfficeOfDestination>
  </ncts:CC042C>

  val cc043c: NodeSeq = <ncts:CC043C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-043</messageSender>
    <messageType>CC043C</messageType>
    <TransitOperation>
      <MRN>MRN-043</MRN>
      <declarationType>declaration-type-043</declarationType>
    </TransitOperation>
    <CustomsOfficeOfDestinationActual>
      <referenceNumber>Newcastle-airport-043</referenceNumber>
    </CustomsOfficeOfDestinationActual>
    <Consignment>
      <countryOfDestination>RO</countryOfDestination>
      <TransportEquipment>
        <numberOfSeals>043</numberOfSeals>
      </TransportEquipment>
      <PreviousDocument>
        <referenceNumber>previous-document-043</referenceNumber>
      </PreviousDocument>
      <HouseConsignment>
        <ConsignmentItem>
          <Packaging>
            <numberOfPackages>043</numberOfPackages> x
          </Packaging>
        </ConsignmentItem>
      </HouseConsignment>
    </Consignment>
  </ncts:CC043C>

  val cc044c: NodeSeq = <ncts:CC044C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-044</messageSender>
    <messageType>CC044C</messageType>
    <TransitOperation>
      <MRN>MRN-044</MRN>
    </TransitOperation>
    <CustomsOfficeOfDestinationActual>
      <referenceNumber>Newcastle-airport-044</referenceNumber>
    </CustomsOfficeOfDestinationActual>
    <Consignment>
      <TransportEquipment>
        <numberOfSeals>044</numberOfSeals>
      </TransportEquipment>
      <HouseConsignment>
        <ConsignmentItem>
          <Packaging>
            <numberOfPackages>044</numberOfPackages>
          </Packaging>
        </ConsignmentItem>
      </HouseConsignment>
    </Consignment>
  </ncts:CC044C>

  val cc045c: NodeSeq = <ncts:CC045C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>message-sender-045</messageSender>
      <messageType>CC045C</messageType>
      <TransitOperation>
        <MRN>MRN-045</MRN>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>Newcastle-airport-045</referenceNumber>
      </CustomsOfficeOfDeparture>
    </ncts:CC045C>

  val cc048c: NodeSeq = <ncts:CC048C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-048</messageSender>
    <messageType>CC048C</messageType>
    <TransitOperation>
      <MRN>MRN-048</MRN>
    </TransitOperation>
    <CustomsOfficeOfDestination>
      <referenceNumber>Paris-048</referenceNumber>
    </CustomsOfficeOfDestination>
  </ncts:CC048C>

  val cc051c: NodeSeq = <ncts:CC051C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-051</messageSender>
    <messageType>CC051C</messageType>
    <TransitOperation>
      <MRN>MRN-051</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-051</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC051C>

  val cc055c: NodeSeq = <ncts:CC055C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-055</messageSender>
    <messageType>CC055C</messageType>
    <TransitOperation>
      <MRN>MRN-055</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-055</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC055C>

  val cc056c: NodeSeq = <ncts:CC056C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-056</messageSender>
    <messageType>CC056C</messageType>
    <TransitOperation>
      <LRN>LRN-056</LRN>
      <MRN>MRN-056</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-056</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC056C>

  val cc057c: NodeSeq = <ncts:CC057C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-057</messageSender>
    <messageType>CC057C</messageType>
    <TransitOperation>
      <MRN>MRN-057</MRN>
    </TransitOperation>
    <CustomsOfficeOfDestinationActual>
      <referenceNumber>Newcastle-airport-057</referenceNumber>
    </CustomsOfficeOfDestinationActual>
  </ncts:CC057C>

  val cc060c: NodeSeq = <ncts:CC060C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-060</messageSender>
    <messageType>CC060C</messageType>
    <TransitOperation>
      <LRN>LRN-060</LRN>
      <MRN>MRN-060</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-060</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC060C>

  val cc140c: NodeSeq = <ncts:CC140C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
      <messageSender>message-sender-140</messageSender>
      <messageType>CC140C</messageType>
      <TransitOperation>
        <MRN>MRN-140</MRN>
      </TransitOperation>
      <CustomsOfficeOfDeparture>
        <referenceNumber>Newcastle-airport-140</referenceNumber>
      </CustomsOfficeOfDeparture>
    </ncts:CC140C>

  val cc141c: NodeSeq = <ncts:CC141C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-141</messageSender>
    <messageType>CC141C</messageType>
    <TransitOperation>
      <MRN>MRN-141</MRN>
    </TransitOperation>
    <CustomsOfficeOfDestinationActual>
      <referenceNumber>Newcastle-airport-141</referenceNumber>
    </CustomsOfficeOfDestinationActual>
  </ncts:CC141C>

  val cc170c: NodeSeq = <ncts:CC170C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-170</messageSender>
    <messageType>CC170C</messageType>
    <TransitOperation>
      <LRN>LRN-170</LRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-170</referenceNumber>
    </CustomsOfficeOfDeparture>
    <Consignment>
      <TransportEquipment>
        <numberOfSeals>170</numberOfSeals>
      </TransportEquipment>
      <LocationOfGoods>
        <CustomsOffice>
          <referenceNumber>170</referenceNumber>
        </CustomsOffice>
        <EconomicOperator>
          <identificationNumber>GB170</identificationNumber>
        </EconomicOperator>
      </LocationOfGoods>
    </Consignment>
  </ncts:CC170C>

  val cc182c: NodeSeq = <ncts:CC182C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-182</messageSender>
    <messageType>CC182C</messageType>
    <TransitOperation>
      <MRN>MRN-182</MRN>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-182</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC182C>

  val cc190c: NodeSeq = <ncts:CC190C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-190</messageSender>
    <messageType>CC190C</messageType>
    <TransitOperation>
      <LRN>LRN-190</LRN>
      <MRN>MRN-190</MRN>
    </TransitOperation>
    <CustomsOfficeOfExit>
      <referenceNumber>Dover-port-190</referenceNumber>
    </CustomsOfficeOfExit>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-190</referenceNumber>
    </CustomsOfficeOfDeparture>
    <Consignment>
      <LocationOfGoods>
        <CustomsOffice>
          <referenceNumber>190</referenceNumber>
        </CustomsOffice>
        <EconomicOperator>
          <identificationNumber>GB190</identificationNumber>
        </EconomicOperator>
      </LocationOfGoods>
    </Consignment>
  </ncts:CC190C>

  val cc191c: NodeSeq = <ncts:CC191C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-191</messageSender>
    <messageType>CC191C</messageType>
    <TransitOperation>
      <LRN>LRN-191</LRN> 
      <MRN>MRN-191</MRN>
    </TransitOperation>
    <CustomsOfficeOfExit>
      <referenceNumber>Dover-port-191</referenceNumber>
    </CustomsOfficeOfExit>
    <CustomsOfficeOfDeparture>
      <referenceNumber>Newcastle-airport-191</referenceNumber>
    </CustomsOfficeOfDeparture>
  </ncts:CC191C>

  val cc228c: NodeSeq = <ncts:CC228C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-228</messageSender>
    <messageType>CC228C</messageType>
    <GuaranteeReference>
      <GRN>guarantee-reference-number-228</GRN>
    </GuaranteeReference>
  </ncts:CC228C>

  val cc906c: NodeSeq = <ncts:CC906C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-906</messageSender>
    <messageType>CC906C</messageType>
    <Header>
      <LRN>LRN-906</LRN>
      <MRN>MRN-906</MRN>
    </Header>
  </ncts:CC906C>

  // cc917c less than 50KB
  // cc928c less than 50KB

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  "XML parsers " - {

    "when a valid CC004C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc004c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE004").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-004")),
            Right(("messageSender", "message-sender-004")),
            Right(("CustomsOffice", "Newcastle-004")),
            Right(("messageType", "CC004C")),
            Right(("MRN", "MRN-004"))
          )
      }
    }

    "when a valid CC007C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc007c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE007").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-1")),
            Right(("EconomicOperator", "GB000001")),
            Right(("CustomsOffice", "Newcastle-1")),
            Right(("messageType", "CC007C")),
            Right(("CustomsOfficeOfDestinationActual", "Newcastle-airport-1")),
            Right(("numberOfSeals", "98")),
            Right(("MRN", "MRN-1"))
          )
      }
    }

    "when a valid CC009C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc009c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE009").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-009")),
            Right(("messageSender", "message-sender-009")),
            Right(("CustomsOffice", "Newcastle-009")),
            Right(("messageType", "CC009C")),
            Right(("MRN", "MRN-009"))
          )
      }
    }

    "when a valid CC013C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc013c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE013").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-013")),
            Right(("declarationType", "declaration-type-013")),
            Right(("messageSender", "message-sender-013")),
            Right(("EconomicOperator", "GB-013")),
            Right(("CountryOfRoutingOfConsignment", "ESP")),
            Right(("messageType", "CC013C")),
            Right(("numberOfPackages", "013")),
            Right(("countryOfDispatch", "UK")),
            Right(("countryOfDestination", "IT")),
            Right(("CustomsOfficeOfTransitDeclared", "Newcastle-port-013")),
            Right(("CustomsOfficeOfDestinationDeclared", "Newcastle-train-station-013")),
            Right(("CustomsOffice", "013")),
            Right(("PreviousDocument", "previous-document-013")),
            Right(("numberOfSeals", "13")),
            Right(("GRN", "guarantee-reference-number-013")),
            Right(("accessCode", "guarantee-access-code-013")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-013")),
            Right(("MRN", "MRN-013"))
          )
      }
    }

    "when a valid CC014C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc014c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE014").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-014")),
            Right(("messageSender", "message-sender-014")),
            Right(("messageType", "CC014C")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-014")),
            Right(("MRN", "MRN-014"))
          )
      }
    }

    "when a valid CC015C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE015").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-015")),
            Right(("declarationType", "declaration-type-015")),
            Right(("messageSender", "message-sender-015")),
            Right(("EconomicOperator", "GB015")),
            Right(("CountryOfRoutingOfConsignment", "CH")),
            Right(("messageType", "CC015C")),
            Right(("countryOfDispatch", "UK")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-015")),
            Right(("countryOfDestination", "IT")),
            Right(("CustomsOfficeOfTransitDeclared", "Newcastle-port-015")),
            Right(("CustomsOfficeOfDestinationDeclared", "Newcastle-train-station-015")),
            Right(("CustomsOffice", "015")),
            Right(("PreviousDocument", "previous-document-015")),
            Right(("numberOfSeals", "015")),
            Right(("GRN", "guarantee-reference-number-015")),
            Right(("accessCode", "guarantee-access-code-015"))
          )
      }
    }

    "when a valid CC017C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc017c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE017").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("declarationType", "declaration-type-017")),
            Right(("messageSender", "message-sender-017")),
            Right(("CountryOfRoutingOfConsignment", "NL")),
            Right(("CustomsOfficeOfDestinationDeclared", "Newcastle-train-station-017")),
            Right(("numberOfPackages", "017")),
            Right(("countryOfDispatch", "UK")),
            Right(("countryOfDestination", "FR")),
            Right(("CustomsOfficeOfTransitDeclared", "Newcastle-port-017")),
            Right(("CustomsOfficeOfExitForTransitDeclared", "Dover-port-017")),
            Right(("messageType", "CC017C")),
            Right(("PreviousDocument", "previous-document-017")),
            Right(("numberOfSeals", "017")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-017")),
            Right(("MRN", "MRN-017"))
          )
      }
    }

    "when a valid CC019C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc019c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE019").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-019")),
            Right(("messageType", "CC019C")),
            Right(("MRN", "MRN-019")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-019"))
          )
      }
    }

    "when a valid CC022C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc022c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE022").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-022")),
            Right(("messageType", "CC022C")),
            Right(("MRN", "MRN-022")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-022"))
          )
      }
    }

    "when a valid CC023C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc023c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE023").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-023")),
            Right(("messageType", "CC023C")),
            Right(("MRN", "MRN-023")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-023"))
          )
      }
    }

    "when a valid CC025C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc025c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE025").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-025")),
            Right(("messageType", "CC025C")),
            Right(("MRN", "MRN-025")),
            Right(("CustomsOfficeOfDestinationActual", "Newcastle-airport-025")),
            Right(("numberOfPackages", "025"))
          )
      }
    }

    "when a valid CC028C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc028c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE028").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-028")),
            Right(("messageSender", "message-sender-028")),
            Right(("CustomsOffice", "Newcastle-airport-028")),
            Right(("messageType", "CC028C")),
            Right(("MRN", "MRN-028"))
          )
      }
    }

    "when a valid CC029C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc029c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE029").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-029")),
            Right(("declarationType", "declaration-type-029")),
            Right(("messageSender", "message-sender-029")),
            Right(("EconomicOperator", "GB029")),
            Right(("CountryOfRoutingOfConsignment", "DE")),
            Right(("numberOfPackages", "029")),
            Right(("countryOfDispatch", "UK")),
            Right(("countryOfDestination", "IT")),
            Right(("CustomsOfficeOfTransitDeclared", "Newcastle-port-029")),
            Right(("CustomsOfficeOfDestinationDeclared", "Newcastle-train-station-029")),
            Right(("CustomsOffice", "029")),
            Right(("CustomsOfficeOfExitForTransitDeclared", "Dover-port-029")),
            Right(("messageType", "CC029C")),
            Right(("PreviousDocument", "previous-document-029")),
            Right(("numberOfSeals", "029")),
            Right(("GRN", "guarantee-reference-number-029")),
            Right(("accessCode", "guarantee-access-code-029")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-029")),
            Right(("MRN", "MRN-029"))
          )
      }
    }

    "when a valid CC035C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc035c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE035").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-035")),
            Right(("messageType", "CC035C")),
            Right(("MRN", "MRN-035")),
            Right(("CustomsOffice", "Newcastle-airport-035"))
          )
      }
    }

    "when a valid CC040C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc040c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE040").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-040")),
            Right(("messageType", "CC040C")),
            Right(("MRN", "MRN-040"))
          )
      }
    }

    "when a valid CC042C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc042c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE042").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("CustomsOfficeOfDestination", "Paris-042")),
            Right(("messageSender", "message-sender-042")),
            Right(("messageType", "CC042C")),
            Right(("CustomsOfficeOfExit", "Dover-port-042")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-042")),
            Right(("MRN", "MRN-042"))
          )
      }
    }

    "when a valid CC043C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc043c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE043").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("declarationType", "declaration-type-043")),
            Right(("messageSender", "message-sender-043")),
            Right(("messageType", "CC043C")),
            Right(("MRN", "MRN-043")),
            Right(("countryOfDestination", "RO")),
            Right(("CustomsOfficeOfDestinationActual", "Newcastle-airport-043")),
            Right(("numberOfPackages", "043")),
            Right(("PreviousDocument", "previous-document-043")),
            Right(("numberOfSeals", "043"))
          )
      }
    }

    "when a valid CC044C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc044c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE044").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-044")),
            Right(("messageType", "CC044C")),
            Right(("numberOfSeals", "044")),
            Right(("MRN", "MRN-044")),
            Right(("CustomsOfficeOfDestinationActual", "Newcastle-airport-044")),
            Right(("numberOfPackages", "044"))
          )
      }
    }

    "when a valid CC045C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc045c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE045").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-045")),
            Right(("messageType", "CC045C")),
            Right(("MRN", "MRN-045")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-045"))
          )
      }
    }

    "when a valid CC048C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc048c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE048").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-048")),
            Right(("messageType", "CC048C")),
            Right(("MRN", "MRN-048")),
            Right(("CustomsOfficeOfDestination", "Paris-048"))
          )
      }
    }

    "when a valid CC051C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc051c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE051").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-051")),
            Right(("messageType", "CC051C")),
            Right(("MRN", "MRN-051")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-051"))
          )
      }
    }

    "when a valid CC055C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc055c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE055").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-055")),
            Right(("messageType", "CC055C")),
            Right(("MRN", "MRN-055")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-055"))
          )
      }
    }

    "when a valid CC056C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc056c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE056").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-056")),
            Right(("messageSender", "message-sender-056")),
            Right(("messageType", "CC056C")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-056")),
            Right(("MRN", "MRN-056"))
          )
      }
    }

    "when a valid CC057C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc057c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE057").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-057")),
            Right(("messageType", "CC057C")),
            Right(("MRN", "MRN-057")),
            Right(("CustomsOfficeOfDestinationActual", "Newcastle-airport-057"))
          )
      }
    }

    "when a valid CC060C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc060c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE060").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-060")),
            Right(("messageSender", "message-sender-060")),
            Right(("messageType", "CC060C")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-060")),
            Right(("MRN", "MRN-060"))
          )
      }
    }

    "when a valid CC140C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc140c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE140").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-140")),
            Right(("messageType", "CC140C")),
            Right(("MRN", "MRN-140")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-140"))
          )
      }
    }

    "when a valid CC141C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc141c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE141").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-141")),
            Right(("messageType", "CC141C")),
            Right(("MRN", "MRN-141")),
            Right(("CustomsOfficeOfDestinationActual", "Newcastle-airport-141"))
          )
      }
    }

    "when a valid CC170C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc170c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE170").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-170")),
            Right(("messageSender", "message-sender-170")),
            Right(("EconomicOperator", "GB170")),
            Right(("CustomsOffice", "170")),
            Right(("messageType", "CC170C")),
            Right(("numberOfSeals", "170")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-170"))
          )
      }
    }

    "when a valid CC182C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc182c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE182").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-182")),
            Right(("messageType", "CC182C")),
            Right(("MRN", "MRN-182")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-182"))
          )
      }
    }

    "when a valid CC190C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc190c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE190").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-190")),
            Right(("messageSender", "message-sender-190")),
            Right(("EconomicOperator", "GB190")),
            Right(("CustomsOffice", "190")),
            Right(("messageType", "CC190C")),
            Right(("CustomsOfficeOfExit", "Dover-port-190")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-190")),
            Right(("MRN", "MRN-190"))
          )
      }
    }

    "when a valid CC191C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc191c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE191").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("LRN", "LRN-191")),
            Right(("messageSender", "message-sender-191")),
            Right(("messageType", "CC191C")),
            Right(("CustomsOfficeOfExit", "Dover-port-191")),
            Right(("CustomsOfficeOfDeparture", "Newcastle-airport-191")),
            Right(("MRN", "MRN-191"))
          )
      }
    }

    "when a valid CC228C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc228c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE228").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-228")),
            Right(("messageType", "CC228C")),
            Right(("GRN", "guarantee-reference-number-228"))
          )
      }
    }

    "when a valid CC906C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc906c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[(String, String)]]] = elementPaths("IE906").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[(String, String)]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right(("messageSender", "message-sender-906")),
            Right(("messageType", "CC906C")),
            Right(("LRN", "LRN-906")),
            Right(("MRN", "MRN-906"))
          )
      }
    }

  }

}
