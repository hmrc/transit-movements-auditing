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

    val cc007c: NodeSeq =
      <ncts:CC007C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
        <messageSender>message-sender-1</messageSender>
        <messageType>CC007C</messageType>
        <TransitOperation>
          <MRN>MRN-1</MRN>
          <arrivalNotificationDateAndTime>2014-06-09T16:15:04+01:00</arrivalNotificationDateAndTime>
        </TransitOperation>
        <!--0 to 9 repetitions:-->
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
          <!--0 to 9 repetitions:-->
          <Incident>
            <!--0 to 9999 repetitions:-->
            <TransportEquipment>
              <!--Optional:-->
              <numberOfSeals>98</numberOfSeals>
              <!--0 to 99 repetitions:-->
            </TransportEquipment>
          </Incident>
        </Consignment>
      </ncts:CC007C>

    val cc015c: NodeSeq = <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
          <messageSender>message-sender-2</messageSender>
          <messageType>CC015C</messageType>
          <TransitOperation>
            <LRN>LRN-2</LRN>
            <declarationType>declaration-type-2</declarationType>
          </TransitOperation>
          <CustomsOfficeOfDeparture>
            <referenceNumber>Newcastle-airport-2</referenceNumber>
          </CustomsOfficeOfDeparture>
          <CustomsOfficeOfDestinationDeclared>
            <referenceNumber>Newcastle-train-station-2</referenceNumber>
          </CustomsOfficeOfDestinationDeclared>
          <CustomsOfficeOfTransitDeclared>
            <sequenceNumber>Newcastle-port-2</sequenceNumber>
          </CustomsOfficeOfTransitDeclared>
      <Guarantee>
        <GuaranteeReference>
          <GRN>guarantee-reference-number-1</GRN>
          <accessCode>guarantee-access-code-1</accessCode>
        </GuaranteeReference>
      </Guarantee>
          <Consignment>
            <countryOfDispatch>UK</countryOfDispatch>
            <countryOfDestination>IT</countryOfDestination>
            <TransportEquipment>
              <numberOfSeals>78</numberOfSeals>
            </TransportEquipment>
            <LocationOfGoods>
              <CustomsOffice>
                <referenceNumber>56</referenceNumber>
              </CustomsOffice>
               <EconomicOperator>
                <identificationNumber>XI1234567</identificationNumber>
              </EconomicOperator>
            </LocationOfGoods>
            <CountryOfRoutingOfConsignment>
              <country>CH</country>
            </CountryOfRoutingOfConsignment>
            <PreviousDocument>
              <referenceNumber>previous-document-2</referenceNumber>
            </PreviousDocument>
          </Consignment>
        </ncts:CC015C>

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

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

    "when a valid CC015C message is provided extract all required elements" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)

      val additionalFields: Iterable[concurrent.Future[ParseResult[String]]] = elementPaths("IE015").map(
        elem => stream.via(XmlParsers.extractElement(elem._1, elem._2)).runWith(Sink.head)
      )

      val result: Future[Iterable[ParseResult[String]]] = concurrent.Future.sequence(additionalFields)

      whenReady(result) {
        additionalParams =>
          additionalParams mustBe List(
            Right("(LRN,LRN-2)"),
            Right("(declarationType,declaration-type-2)"),
            Right("(messageSender,message-sender-2)"),
            Right("(EconomicOperator,XI1234567)"),
            Right("(CountryOfRoutingOfConsignment,CH)"),
            Right("(messageType,CC015C)"),
            Right("(countryOfDispatch,UK)"),
            Right("(CustomsOfficeOfDeparture,Newcastle-airport-2)"),
            Right("(countryOfDestination,IT)"),
            Right("(CustomsOfficeOfTransitDeclared,Newcastle-port-2)"),
            Right("(CustomsOfficeOfDestinationDeclared,Newcastle-train-station-2)"),
            Right("(CustomsOffice,56)"),
            Right("(PreviousDocument,previous-document-2)"),
            Right("(numberOfSeals,78)"),
            Right("(GRN,guarantee-reference-number-1)"),
            Right("(accessCode,guarantee-access-code-1)")
          )
      }
    }

    "when a valid CC015C message is provided extract messageSender element" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("IE015")
      val result                        = stream.via(XmlParsers.extractElement("messageSender", paths("messageSender"))).runWith(Sink.head)
      whenReady(result) {
        value =>
          value mustBe Right("(messageSender,message-sender-2)")
      }
    }

    "when a valid CC015C message is provided extract LRN element" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("IE015")
      val result                        = stream.via(XmlParsers.extractElement("LRN", paths("LRN"))).runWith(Sink.head)
      whenReady(result) {
        lrn =>
          lrn mustBe Right("(LRN,LRN-2)")
      }
    }

    "when a valid CC015C message is provided extract declaration type element" in {

      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("IE015")
      val result                        = stream.via(XmlParsers.extractElement("declarationType", paths("declarationType"))).runWith(Sink.head)
      whenReady(result) {
        declaration =>
          declaration mustBe Right("(declarationType,declaration-type-2)")
      }
    }

    "when a valid CC015C message is provided extract CustomsOfficeOfDeparture element" in {
      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("IE015")

      val result =
        stream.via(XmlParsers.extractElement("CustomsOfficeOfDeparture", paths("CustomsOfficeOfDeparture"))).runWith(Sink.head)
      whenReady(result) {
        office =>
          office mustBe Right("(CustomsOfficeOfDeparture,Newcastle-airport-2)")
      }
    }

    "when a valid CC015C message is provided extract CustomsOfficeOfDestinationDeclared element" in {
      val stream: Source[ParseEvent, _] = createParsingEventStream(cc015c)
      val paths                         = elementPaths("IE015")

      val result =
        stream.via(XmlParsers.extractElement("CustomsOfficeOfDestinationDeclared", paths("CustomsOfficeOfDestinationDeclared"))).runWith(Sink.head)
      whenReady(result) {
        office =>
          office mustBe Right("(CustomsOfficeOfDestinationDeclared,Newcastle-train-station-2)")
      }
    }

  }

}
