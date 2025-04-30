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

import cats.data.EitherT
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.base.TestStreamComponents
import uk.gov.hmrc.transitmovementsauditing.connectors.ConversionConnector
import uk.gov.hmrc.transitmovementsauditing.models.MessageType
import uk.gov.hmrc.transitmovementsauditing.models.errors.ConversionError
import uk.gov.hmrc.transitmovementsauditing.services.ConversionServiceImpl

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConversionServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with TestActorSystem with ScalaFutures {

  "When a valid stream is provided" - {
    "assure the stream is consumed and returns a valid JSON ByteString based stream" in {

      val mockConversionConnector = mock[ConversionConnector]
      val dummyJsonSource: EitherT[Future, Throwable, Source[ByteString, ?]] =
        EitherT.rightT(Source.single(ByteString("""{ "dummy": "dummy" }""")))
      when(mockConversionConnector.postXml(any(), any())(any())).thenReturn(dummyJsonSource)

      val sut = new ConversionServiceImpl(mockConversionConnector)

      // Because we can't spy on the source and test we ran the system (and I can't find a way to test the
      // stream was materialised) and data flows, what we do here is attach a dummy flow object that contains a
      // Future[Done] as a materialised value. When the pre-materialisation occurs, we get the future,
      // which will contain a Done once the data starts flowing - so we look out for a completed future
      // here.
      val (monitor, source) =
        Source.single(ByteString(<test></test>.mkString, StandardCharsets.UTF_8)).viaMat(TestStreamComponents.flowProbe)(Keep.right).preMaterialize()

      val messageType: MessageType   = MessageType.IE015
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val newSource = sut.toJson(messageType, source)
      val sink = newSource.flatMap(
        x => EitherT.right[ConversionError](x.runWith(Sink.head))
      )

      whenReady(monitor) {
        _ =>
          whenReady(sink.value) {
            case Right(result) =>
              Json.parse(result.utf8String) mustBe Json.obj("dummy" -> "dummy")
              monitor.isCompleted mustBe true
            case _ => fail("Failed to get a source")
          }
      }
    }

    "When an invalid stream is provided" - {
      "assure we get a ConversionError#FailedConversion with the appropriate message" in {

        val mockConversionConnector = mock[ConversionConnector]
        val dummyJsonSource: EitherT[Future, Throwable, Source[ByteString, ?]] =
          EitherT.leftT(UpstreamErrorResponse("""{ "code": "BAD_REQUEST", "message": "error" }""", 400))
        when(mockConversionConnector.postXml(any(), any())(any())).thenReturn(dummyJsonSource)

        val sut = new ConversionServiceImpl(mockConversionConnector)

        // Because we can't spy on the source and test we ran the system (and I can't find a way to test the
        // stream was materialised) and data flows, what we do here is attach a dummy flow object that contains a
        // Future[Done] as a materialised value. When the pre-materialisation occurs, we get the future,
        // which will contain a Done once the data starts flowing - so we look out for a completed future
        // here.
        val (monitor, source) =
          Source.single(ByteString(<test></test>.mkString, StandardCharsets.UTF_8)).viaMat(TestStreamComponents.flowProbe)(Keep.right).preMaterialize()

        val messageType: MessageType   = MessageType.IE015
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val newSource = sut.toJson(messageType, source)

        whenReady(newSource.value) {
          result =>
            monitor.isCompleted mustBe true
            result match {
              case Left(ConversionError.FailedConversion("error")) => ()
              case Left(ConversionError.FailedConversion(x))       => fail(s"""Incorrect message: got "$x", should be "error".""")
              case _                                               => fail("Incorrect response")
            }
        }
      }

      "When an unknown error occurs" - {
        "assure we get a ConversionError#UnknownError with the appropriate message" in {

          val mockConversionConnector = mock[ConversionConnector]
          val dummyJsonSource: EitherT[Future, Throwable, Source[ByteString, ?]] =
            EitherT.leftT(UpstreamErrorResponse("""{ "code": "INTERNAL_SERVER_ERROR", "message": "error" }""", 500))
          when(mockConversionConnector.postXml(any(), any())(any())).thenReturn(dummyJsonSource)

          val sut = new ConversionServiceImpl(mockConversionConnector)

          // Because we can't spy on the source and test we ran the system (and I can't find a way to test the
          // stream was materialised) and data flows, what we do here is attach a dummy flow object that contains a
          // Future[Done] as a materialised value. When the pre-materialisation occurs, we get the future,
          // which will contain a Done once the data starts flowing - so we look out for a completed future
          // here.
          val (monitor, source) =
            Source.single(ByteString(<test></test>.mkString, StandardCharsets.UTF_8)).viaMat(TestStreamComponents.flowProbe)(Keep.right).preMaterialize()

          val messageType: MessageType   = MessageType.IE015
          implicit val hc: HeaderCarrier = HeaderCarrier()

          val newSource = sut.toJson(messageType, source)

          whenReady(
            monitor.flatMap(
              _ => newSource.value
            )
          ) {
            case Left(ConversionError.UnexpectedError(_, Some(UpstreamErrorResponse(_, 500, _, _)))) => ()
            case _                                                                                   => fail("Incorrect response")
          }
        }
      }
    }
  }
}
