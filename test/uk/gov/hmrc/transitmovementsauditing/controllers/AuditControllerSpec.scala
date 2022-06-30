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

package uk.gov.hmrc.transitmovementsauditing.controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.reset
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpErrorConfig
import play.api.http.Status
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.mvc.Headers
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.AmendmentAcceptance
import uk.gov.hmrc.transitmovementsauditing.models.errors.ConversionError
import uk.gov.hmrc.transitmovementsauditing.services.AuditService
import uk.gov.hmrc.transitmovementsauditing.services.ConversionService

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditControllerSpec extends AnyFreeSpec with Matchers with TestActorSystem with MockitoSugar with BeforeAndAfterEach {

  private val fakeRequest = FakeRequest(
    "POST",
    "/",
    Headers(CONTENT_TYPE -> "application/xml"),
    Source.single(ByteString(<test></test>.mkString, StandardCharsets.UTF_8))
  )

  private val mockAuditService      = mock[AuditService]
  private val mockConversionService = mock[ConversionService]

  private val conversionServiceXmlToJsonPartial: PartialFunction[Any, EitherT[Future, ConversionError, Source[ByteString, _]]] = {
    case _ => EitherT.rightT(Source.single(ByteString(Json.stringify(Json.obj("dummy" -> "dummy")))))
  }

  val errorHandler = new DefaultHttpErrorHandler(HttpErrorConfig(showDevErrors = false, None), None, None)

  val controllerComponentWithTempFile: ControllerComponents =
    stubControllerComponents(playBodyParsers = PlayBodyParsers(SingletonTemporaryFileCreator, errorHandler)(materializer))

  private val controller = new AuditController(controllerComponentWithTempFile, mockConversionService, mockAuditService)(materializer)

  override def beforeEach(): Unit = {
    reset(mockConversionService)
    reset(mockAuditService)
  }

  "POST /" - {
    "return 202 when auditing was successful" in {
      when(mockConversionService.toJson(any())(any(), any())).thenAnswer(conversionServiceXmlToJsonPartial)
      when(mockAuditService.send(any(), any())).thenReturn(EitherT.rightT(()))

      val result = controller.post(AmendmentAcceptance)(fakeRequest)
      status(result) shouldBe Status.ACCEPTED
    }
  }
}
