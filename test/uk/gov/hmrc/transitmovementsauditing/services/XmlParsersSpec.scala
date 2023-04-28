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
    with ElementPaths
    with TestActorSystem
    with Matchers
    with StreamTestHelpers
    with BeforeAndAfterEach
    with ScalaFutures
    with ScalaCheckDrivenPropertyChecks {

  "Movement Reference Number parser" - {

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

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    "when a valid CC004C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc004c)

      implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

      val additionalFields: Iterable[concurrent.Future[ParseResult[String]]] = elementPaths("IE004").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right("(LRN,LRN-004)"),
            Right("(messageSender,message-sender-004)"),
            Right("(CustomsOffice,Newcastle-004)"),
            Right("(messageType,CC004C)"),
            Right("(MRN,MRN-004)")
          )
      }
    }

    "when a valid CC007C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc007c)

      implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

      val additionalFields: Iterable[concurrent.Future[ParseResult[String]]] = elementPaths("IE007").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right("(messageSender,message-sender-1)"),
            Right("(EconomicOperator,GB000001)"),
            Right("(CustomsOffice,Newcastle-1)"),
            Right("(messageType,CC007C)"),
            Right("(CustomsOfficeOfDestinationActual,Newcastle-airport-1)"),
            Right("(numberOfSeals,98)"),
            Right("(MRN,MRN-1)")
          )
      }
    }

    "when a valid CC009C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc009c)

      implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

      val additionalFields: Iterable[concurrent.Future[ParseResult[String]]] = elementPaths("IE009").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right("(LRN,LRN-009)"),
            Right("(messageSender,message-sender-009)"),
            Right("(CustomsOffice,Newcastle-009)"),
            Right("(messageType,CC009C)"),
            Right("(MRN,MRN-009)")
          )
      }
    }

    "when a valid CC013C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc013c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[String]]] = elementPaths("IE013").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[String]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right("(LRN,LRN-013)"),
            Right("(declarationType,declaration-type-013)"),
            Right("(messageSender,message-sender-013)"),
            Right("(EconomicOperator,GB-013)"),
            Right("(CountryOfRoutingOfConsignment,ESP)"),
            Right("(messageType,CC013C)"),
            Right("(numberOfPackages,013)"),
            Right("(countryOfDispatch,UK)"),
            Right("(countryOfDestination,IT)"),
            Right("(CustomsOfficeOfTransitDeclared,Newcastle-port-013)"),
            Right("(CustomsOfficeOfDestinationDeclared,Newcastle-train-station-013)"),
            Right("(CustomsOffice,013)"),
            Right("(PreviousDocument,previous-document-013)"),
            Right("(numberOfSeals,13)"),
            Right("(GRN,guarantee-reference-number-013)"),
            Right("(accessCode,guarantee-access-code-013)"),
            Right("(CustomsOfficeOfDeparture,Newcastle-airport-013)"),
            Right("(MRN,MRN-013)")
          )
      }
    }

    "when a valid CC015C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[String]]] = elementPaths("IE015").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[String]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right("(LRN,LRN-015)"),
            Right("(declarationType,declaration-type-015)"),
            Right("(messageSender,message-sender-015)"),
            Right("(EconomicOperator,GB015)"),
            Right("(CountryOfRoutingOfConsignment,CH)"),
            Right("(messageType,CC015C)"),
            Right("(countryOfDispatch,UK)"),
            Right("(CustomsOfficeOfDeparture,Newcastle-airport-015)"),
            Right("(countryOfDestination,IT)"),
            Right("(CustomsOfficeOfTransitDeclared,Newcastle-port-015)"),
            Right("(CustomsOfficeOfDestinationDeclared,Newcastle-train-station-015)"),
            Right("(CustomsOffice,015)"),
            Right("(PreviousDocument,previous-document-015)"),
            Right("(numberOfSeals,015)"),
            Right("(GRN,guarantee-reference-number-015)"),
            Right("(accessCode,guarantee-access-code-015)")
          )
      }
    }

  }

}
