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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.reset
import org.mockito.MockitoSugar.when
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
import uk.gov.hmrc.transitmovementsauditing.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationData
import uk.gov.hmrc.transitmovementsauditing.models.MessageType.IE015
import uk.gov.hmrc.transitmovementsauditing.models._
import uk.gov.hmrc.transitmovementsauditing.models.MovementType.Departure
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

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

  private val someGoodCC015CJson = Json.obj("messageSender" -> "sender")

  private val metadataMessageType: Metadata =
    Metadata("some-path", None, Some(MovementId("movementId")), Some(MessageId("messageId")), Some(EORINumber("enrolmentEORI")), Some(Departure), Some(IE015))

  private val metadataFailedStatusType: Metadata =
    Metadata(
      "some-path",
      Some("CTCTradersFailed"),
      Some(MovementId("movementId")),
      Some(MessageId("messageId")),
      Some(EORINumber("enrolmentEORI")),
      Some(Departure),
      Some(IE015)
    )

  private val metadataSuccessStatusType: Metadata =
    Metadata(
      "some-path",
      Some("CTCTradersSucceeded"),
      Some(MovementId("movementId")),
      Some(MessageId("messageId")),
      Some(EORINumber("enrolmentEORI")),
      Some(Departure),
      Some(IE015)
    )

  private val metadataWorkflowStatusType: Metadata =
    Metadata(
      "some-path",
      Some("CTCTradersWorkflow"),
      Some(MovementId("movementId")),
      Some(MessageId("messageId")),
      Some(EORINumber("enrolmentEORI")),
      Some(Departure),
      Some(IE015)
    )

  private val statusTypeFailedEventDetails = Details(metadataFailedStatusType, Some(someGoodCC015CJson))

  private val statusTypeSuccessEventDetails  = Details(metadataSuccessStatusType, Some(someGoodCC015CJson))
  private val statusTypeWorkflowEventDetails = Details(metadataWorkflowStatusType, Some(someGoodCC015CJson))

  private val messageTypeValidDetails = Details(metadataMessageType, Some(someGoodCC015CJson))

  private val detailsWithEmptyPayload = Details(metadataMessageType, None)

  override def beforeEach: Unit = reset(mockAuditConnector)

  "message type audit" - {

    "should successfully send message to audit connector" - AuditType.messageTypeEvents.foreach {
      auditType =>
        s"${auditType.name} to ${auditType.source}" in {
          reset(mockAuditConnector)
          val service                                   = new AuditServiceImpl(mockAuditConnector)
          val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

          when(mockAuditConnector.sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Success))

          val result = service.sendMessageTypeEvent(auditType, messageTypeValidDetails)

          whenReady(result.value, Timeout(1.second)) {
            result =>
              result mustBe Right(())
              val extendedDataEvent = captor.getValue
              extendedDataEvent.auditType mustBe auditType.name
              extendedDataEvent.auditSource mustBe auditType.source
              extendedDataEvent.detail mustBe Json.toJson(messageTypeValidDetails)
          }
        }
    }

    "should return an error when the connector reports a failure" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Failure("a failure")))

      val result = service.sendMessageTypeEvent(DeclarationData, messageTypeValidDetails)

      whenReady(result.value, Timeout(1.second)) {
        _.left.getOrElse(Failure("a different failure")) mustBe a[AuditError.UnexpectedError]
      }
    }

    "should return an error when the connector reports that auditing is disabled" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Disabled))

      val result = service.sendMessageTypeEvent(DeclarationData, messageTypeValidDetails)

      whenReady(result.value, Timeout(1.second)) {
        _ mustBe Left(AuditError.Disabled)
      }
    }

    "should successfully send a message with an empty payload to the audit connector" in {

      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Success))

      val service = new AuditServiceImpl(mockAuditConnector)
      val result  = service.sendMessageTypeEvent(DeclarationData, detailsWithEmptyPayload)

      whenReady(result.value, Timeout(1.second)) {
        result =>
          result mustBe Right(())
          val extendedDataEvent = captor.getValue
          extendedDataEvent.auditType mustBe DeclarationData.name
          extendedDataEvent.auditSource mustBe DeclarationData.source
          extendedDataEvent.detail mustBe Json.toJson(detailsWithEmptyPayload)
      }
    }
  }

  "status audit" - {
    "should successfully send message to audit connector for failed event" - AuditType.auditFailedEvent.foreach {
      auditType =>
        s"${auditType.name} to ${auditType.source}" in {
          reset(mockAuditConnector)
          val service                                   = new AuditServiceImpl(mockAuditConnector)
          val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

          when(mockAuditConnector.sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Success))

          val result = service.sendStatusTypeEvent(statusTypeFailedEventDetails, auditType.parent.get, auditType.source)

          whenReady(result.value, Timeout(1.second)) {
            result =>
              result mustBe Right(())
              val extendedDataEvent = captor.getValue
              extendedDataEvent.auditType mustBe auditType.parent.get
              extendedDataEvent.auditSource mustBe auditType.source
              Json.parse(extendedDataEvent.detail.toString()).validate[Details].get mustBe statusTypeFailedEventDetails
          }
        }
    }

    "should successfully send message to audit connector for success event" - AuditType.auditSuccessEvent.foreach {
      auditType =>
        s"${auditType.name} to ${auditType.source}" in {
          reset(mockAuditConnector)
          val service                                   = new AuditServiceImpl(mockAuditConnector)
          val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

          when(mockAuditConnector.sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Success))

          val result = service.sendStatusTypeEvent(statusTypeSuccessEventDetails, auditType.parent.get, auditType.source)

          whenReady(result.value, Timeout(1.second)) {
            result =>
              result mustBe Right(())
              val extendedDataEvent = captor.getValue
              extendedDataEvent.auditType mustBe auditType.parent.get
              extendedDataEvent.auditSource mustBe auditType.source
              Json.parse(extendedDataEvent.detail.toString()).validate[Details].get mustBe statusTypeSuccessEventDetails
          }
        }
    }

    "should successfully send message to audit connector for failed event" - AuditType.auditWorkFlowEvent.foreach {
      auditType =>
        s"${auditType.name} to ${auditType.source}" in {
          reset(mockAuditConnector)
          val service                                   = new AuditServiceImpl(mockAuditConnector)
          val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

          when(mockAuditConnector.sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Success))

          val result = service.sendStatusTypeEvent(statusTypeWorkflowEventDetails, auditType.parent.get, auditType.source)

          whenReady(result.value, Timeout(1.second)) {
            result =>
              result mustBe Right(())
              val extendedDataEvent = captor.getValue
              extendedDataEvent.auditType mustBe auditType.parent.get
              extendedDataEvent.auditSource mustBe auditType.source
              Json.parse(extendedDataEvent.detail.toString()).validate[Details].get mustBe statusTypeWorkflowEventDetails
          }
        }
    }

    "should return an error when the connector reports a failure" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Failure("a failure")))

      val result = service.sendStatusTypeEvent(statusTypeFailedEventDetails, "CTCTradersFailed", "common-transit-convention-traders")

      whenReady(result.value, Timeout(1.second)) {
        _.left.getOrElse(Failure("a different failure")) mustBe a[AuditError.UnexpectedError]
      }
    }

    "should return an error when the connector reports that auditing is disabled" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Disabled))

      val result = service.sendStatusTypeEvent(statusTypeFailedEventDetails, "CTCTradersFailed", "common-transit-convention-traders")

      whenReady(result.value, Timeout(1.second)) {
        _ mustBe Left(AuditError.Disabled)
      }
    }

  }

}
