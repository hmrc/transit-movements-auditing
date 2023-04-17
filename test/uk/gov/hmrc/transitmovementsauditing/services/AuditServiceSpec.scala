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
import uk.gov.hmrc.transitmovementsauditing.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationData
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
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
