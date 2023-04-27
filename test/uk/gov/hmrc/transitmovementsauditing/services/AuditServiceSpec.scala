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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.reset
import org.mockito.MockitoSugar.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Disabled
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Failure
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.transitmovementsauditing.Payload
import uk.gov.hmrc.transitmovementsauditing.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationData
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError
import uk.gov.hmrc.transitmovementsauditing.models.errors.ParseError

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq

class AuditServiceSpec
    extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with TestActorSystem
    with StreamTestHelpers
    with BeforeAndAfterEach
    with ScalaCheckDrivenPropertyChecks {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockAuditConnector: AuditConnector = mock[AuditConnector]

  private val cc015c: NodeSeq = <ncts:CC015C PhaseID="NCTS5.0" xmlns:ncts="http://ncts.dgtaxud.ec">
    <messageSender>message-sender-1</messageSender>
    <messageRecipient>token</messageRecipient>
    <preparationDateAndTime>2007-10-26T07:36:28</preparationDateAndTime>
    <messageIdentification>token</messageIdentification>
    <messageType>CD975C</messageType>
    <!--Optional:-->
    <correlationIdentifier>token</correlationIdentifier>
    <TransitOperation>
      <LRN>LRN-1</LRN>
      <declarationType>declaration-type-1</declarationType>
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
      <referenceNumber>customs-office-of-departure-1</referenceNumber>
    </CustomsOfficeOfDeparture>
    <CustomsOfficeOfDestinationDeclared>
      <referenceNumber>customs-office-of-destination-declared-1</referenceNumber>
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
          <declarationType>token</declarationType>
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

  private val someGoodCC015CJson =
    Json.obj("messageSender" -> "sender")

  private val someGoodCC015CJsonString = Json.stringify(someGoodCC015CJson)

  private val someInvalidJson =
    """{
      |  "messageSender":
      |}""".stripMargin

  override def beforeEach: Unit =
    reset(mockAuditConnector)

  "Audit service" - {

    "should return the additional field for CustomsOfficeOfDeparture" in {
      val service                       = new AuditServiceImpl(mockAuditConnector)
      val stream: Source[ByteString, _] = createStream(cc015c)
      val result: Future[Either[ParseError, String]] =
        service.getAdditionalField("CustomsOfficeOfDeparture", "CC015C" :: "CustomsOfficeOfDeparture" :: "referenceNumber" :: Nil, stream)

      whenReady(result, Timeout(1.second)) {
        result =>
          result mustBe Right("(CustomsOfficeOfDeparture,customs-office-of-departure-1)")
      }
    }

    "should return all the additional fields for message type CC015C " in {
      val service                                     = new AuditServiceImpl(mockAuditConnector)
      val stream: Source[ByteString, _]               = createStream(cc015c)
      val result: EitherT[Future, ParseError, String] = service.getAdditionalFields(DeclarationData.messageType, stream)

      val expected =
        "(LRN,LRN-1)(declarationType,declaration-type-1)(messageSender,message-sender-1)(CustomsOfficeOfDestinationDeclared,customs-office-of-destination-declared-1)(CustomsOfficeOfDeparture,customs-office-of-departure-1)".stripMargin

      whenReady(result.value, Timeout(1.second)) {
        result =>
          result mustBe Right(expected)
      }
    }

    "should successfully send message to audit connector" - AuditType.values.foreach {
      auditType =>
        s"${auditType.name} to ${auditType.source}" in {
          // testing the reduce works as expected by splitting the string into pieces.
          val pieces: Int = Gen.oneOf(1 to 4).sample.getOrElse(1)
          reset(mockAuditConnector)
          val service                                   = new AuditServiceImpl(mockAuditConnector)
          val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

          when(mockAuditConnector.sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Success))

          val result = service.send(auditType, Right(createStream(someGoodCC015CJsonString, pieces)))

          whenReady(result.value, Timeout(1.second)) {
            result =>
              result mustBe Right(())
              val extendedDataEvent = captor.getValue
              extendedDataEvent.auditType mustBe auditType.name
              extendedDataEvent.auditSource mustBe auditType.source
              extendedDataEvent.detail mustBe someGoodCC015CJson
          }
        }
    }

    "should return an error when the service fails to parse invalid json" in {
      val service = new AuditServiceImpl(mockAuditConnector)
      val result  = service.send(DeclarationData, Right(createStream(someInvalidJson)))

      whenReady(result.value, Timeout(1.second)) {
        res =>
          res.isLeft mustBe true
          res.left.getOrElse(AuditError.UnexpectedError) mustBe a[AuditError.FailedToParse]
      }
    }

    "should return an error when the connector reports a failure" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Failure("a failure")))

      val result = service.send(DeclarationData, Right(createStream(someGoodCC015CJsonString)))

      whenReady(result.value, Timeout(1.second)) {
        _.left.getOrElse(Failure("a different failure")) mustBe a[AuditError.UnexpectedError]
      }
    }

    "should return an error when the connector reports that auditing is disabled" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Disabled))

      val result = service.send(DeclarationData, Right(createStream(someGoodCC015CJsonString)))

      whenReady(result.value, Timeout(1.second)) {
        _ mustBe Left(AuditError.Disabled)
      }
    }

    "should return an error if an empty stream is provided" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Disabled))

      val result = service.send(DeclarationData, Right(Source.empty))

      whenReady(result.value, Timeout(1.second)) {
        case Left(AuditError.UnexpectedError(message, _)) =>
          message mustBe "Error extracting body from stream"
        case x => fail(s"Did not get a Left from an unexpected exception - got $x")
      }
    }
  }

}
