/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Failure
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.transitmovementsauditing.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationData

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class AuditServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with TestActorSystem with StreamTestHelpers with BeforeAndAfterEach {
  implicit val hc = HeaderCarrier()
  implicit val ec = ExecutionContext.Implicits.global

  private val mockAuditConnector: AuditConnector = mock[AuditConnector]

  private val someGoodCC015CJson =
    """{
      |  "messageSender": "sender"
      |}""".stripMargin

  private val someInvalidJson =
    """{
      |  "messageSender":
      |}""".stripMargin

  override def beforeEach: Unit =
    reset(mockAuditConnector)

  "Audit service" - {

    "should successfully send message to audit connector" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Success))

      val result = service.send(DeclarationData, createStream(someGoodCC015CJson))

      whenReady(result.value, Timeout(1.second)) {
        _ mustBe Right(())
      }
    }

    "should fail to parse invalid json" in {
      val service = new AuditServiceImpl(mockAuditConnector)
      val result  = service.send(DeclarationData, createStream(someInvalidJson))

      whenReady(result.value) {
        res =>
          res.isLeft mustBe true
          res.left.get must not be ""
      }
    }

    "should return Left Failure" in {
      val service = new AuditServiceImpl(mockAuditConnector)

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Failure("a failure")))

      val result = service.send(DeclarationData, createStream(someGoodCC015CJson))

      whenReady(result.value, Timeout(1.second)) {
        _ mustBe Left("a failure")
      }
    }
  }

}
