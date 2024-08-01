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

package uk.gov.hmrc.transitmovementsauditing.v2_1.services

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transitmovementsauditing.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.AuditType.ArrivalNotification
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.AuditType.DeclarationData
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ParseError.NoElementFound

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq

class FieldParsingServiceSpec
    extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with TestActorSystem
    with StreamTestHelpers
    with BeforeAndAfterEach
    with ScalaCheckDrivenPropertyChecks {

  private val cc015c: NodeSeq = <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-1</messageSender>
    <messageType>CC0015C</messageType>
    <TransitOperation>
      <LRN>LRN-1</LRN>
      <declarationType>declaration-type-1</declarationType>
    </TransitOperation>
    <CustomsOfficeOfDeparture>
      <referenceNumber>customs-office-of-departure-1</referenceNumber>
    </CustomsOfficeOfDeparture>
    <CustomsOfficeOfDestinationDeclared>
      <referenceNumber>customs-office-of-destination-declared-1</referenceNumber>
    </CustomsOfficeOfDestinationDeclared>
    <CustomsOfficeOfTransitDeclared>
      <sequenceNumber>2</sequenceNumber>
    </CustomsOfficeOfTransitDeclared>
    <Guarantee>
      <GuaranteeReference>
        <GRN>guarantee-reference-number-1</GRN>
        <accessCode>guarantee-access-code-1</accessCode>
      </GuaranteeReference>
    </Guarantee>
    <Consignment>
      <countryOfDispatch>UK</countryOfDispatch>
      <countryOfDestination>DE</countryOfDestination>
      <TransportEquipment>
        <numberOfSeals>67</numberOfSeals>
      </TransportEquipment>
      <LocationOfGoods>
        <CustomsOffice>
          <referenceNumber>Hamburg</referenceNumber>
        </CustomsOffice>
        <EconomicOperator>
          <identificationNumber>GB12345678</identificationNumber>
        </EconomicOperator>
      </LocationOfGoods>
      <CountryOfRoutingOfConsignment>
        <country>CH</country>
      </CountryOfRoutingOfConsignment>
      <PreviousDocument>
        <referenceNumber>previous-document-ref-1</referenceNumber>
      </PreviousDocument>
    </Consignment>
  </ncts:CC015C>

  "FieldParsingService" - {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val paths                      = new ElementPaths {}
    val service                    = new FieldParsingServiceImpl()

    "should return the additional field for CustomsOfficeOfDeparture" in {
      val stream: Source[ByteString, _] = createStream(cc015c)
      val result                        = service.getAdditionalField("CustomsOfficeOfDeparture", paths.customsOfficeOfDepartureFor("CC015C"), stream)

      whenReady(result, Timeout(1.second)) {
        result =>
          result mustBe Right(("CustomsOfficeOfDeparture", "customs-office-of-departure-1"))
      }
    }

    "should return element not found for an incorrect path" in {
      val stream: Source[ByteString, _] = createStream(cc015c)
      val result                        = service.getAdditionalField("CustomsOfficeOfDeparture", paths.customsOfficeOfDepartureFor("NonExistentMessage"), stream)

      whenReady(result, Timeout(1.second)) {
        result =>
          result mustBe Left(NoElementFound("CustomsOfficeOfDeparture"))
      }
    }

    "should return all the additional fields for message type CC015C " in {
      val stream: Source[ByteString, _] = createStream(cc015c)
      val result                        = service.getAdditionalFields(DeclarationData.messageType, stream)

      val expected =
        List(
          Right("LRN", "LRN-1"),
          Right("declarationType", "declaration-type-1"),
          Right("messageSender", "message-sender-1"),
          Right("EconomicOperator", "GB12345678"),
          Right("CountryOfRoutingOfConsignment", "CH"),
          Right("messageType", "CC0015C"),
          Right("countryOfDispatch", "UK"),
          Right("CustomsOfficeOfDeparture", "customs-office-of-departure-1"),
          Right("countryOfDestination", "DE"),
          Right("CustomsOfficeOfTransitDeclared", "2"),
          Right("CustomsOfficeOfDestinationDeclared", "customs-office-of-destination-declared-1"),
          Right("CustomsOffice", "Hamburg"),
          Right("PreviousDocument", "previous-document-ref-1"),
          Right("numberOfSeals", "67"),
          Right("GRN", "guarantee-reference-number-1"),
          Right("accessCode", "guarantee-access-code-1")
        )

      whenReady(result.value, Timeout(1.second)) {
        result =>
          result mustBe Right(expected)
      }
    }

    "should not find Arrival notification (cc007c) elements in a cc015c stream" in {
      val stream: Source[ByteString, _] = createStream(cc015c)
      val result                        = service.getAdditionalFields(ArrivalNotification.messageType, stream)

      val expected =
        List(
          Left(NoElementFound("messageSender")),
          Left(NoElementFound("EconomicOperator")),
          Left(NoElementFound("CustomsOffice")),
          Left(NoElementFound("messageType")),
          Left(NoElementFound("CustomsOfficeOfDestinationActual")),
          Left(NoElementFound("numberOfSeals")),
          Left(NoElementFound("MRN"))
        )

      whenReady(result.value, Timeout(1.second)) {
        result =>
          result mustBe Right(expected)
      }
    }

  }
}
