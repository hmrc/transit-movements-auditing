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

package uk.gov.hmrc.transitmovementsauditing.v2_1.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.ConfigFactory
import io.lemonlabs.uri.Url
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.ahc._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.client.HttpClientV2Impl
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.transitmovementsauditing.itbase.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.itbase.WiremockSuite
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.MessageType

import scala.concurrent.Future

class ConversionConnectorSpec extends AnyFreeSpec with Matchers with MockitoSugar with WiremockSuite with ScalaFutures with TestActorSystem {

  implicit val ec     = materializer.executionContext
  private val timeout = Timeout(5.seconds)

  private def conversionUrl(messageType: String) = s"/transit-movements-converter/messages/$messageType"

  val appConfig      = mock[AppConfig]
  lazy val serverUrl = Url.parse(server.baseUrl())
  when(appConfig.converterUrl).thenAnswer(serverUrl)

  lazy val httpClientV2: HttpClientV2 = {
    val config = Configuration(ConfigFactory.load())
    new HttpClientV2Impl(
      wsClient = AhcWSClient(AhcWSClientConfigFactory.forConfig(config.underlying)),
      TestActorSystem.system,
      config,
      hooks = Seq.empty
    )
  }

  val sut = new ConversionConnectorImpl(appConfig, httpClientV2)

  "On a successful conversion, return a 200 (OK) with the stream" in {

    val success = Json.obj("success" -> "success")

    server.stubFor(
      post(
        urlEqualTo(conversionUrl("IE015"))
      )
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.XML))
        .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.JSON))
        .withHeader(Constants.APIVersionHeaderKey, equalTo(Constants.APIVersionFinalHeaderValue))
        .willReturn(
          aResponse().withStatus(OK).withBody(Json.stringify(success))
        )
    )

    val stream = Source.fromIterator(
      () => Seq(ByteString("<test>"), ByteString("</test>")).iterator
    )
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val result = sut
      .postXml(MessageType.IE015, stream)
      .semiflatMap(
        r =>
          r.reduce(_ ++ _)
            .via(Flow.fromFunction(_.utf8String))
            .via(Flow.fromFunction(Json.parse))
            .runWith(Sink.head)
      )

    whenReady(result.value, timeout) {
      case Right(x) => x mustBe success
      case Left(x)  => fail("There should not have been an error", x)
    }

  }

  "On a failed conversion, return a 400 (Bad Request)" in {

    val body = Json.obj("code" -> "BAD_REQUEST", "message" -> "Bad Request")

    server.stubFor(
      post(
        urlEqualTo(conversionUrl("IE015"))
      )
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.XML))
        .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.JSON))
        .withHeader(Constants.APIVersionHeaderKey, equalTo(Constants.APIVersionFinalHeaderValue))
        .willReturn(
          aResponse().withStatus(BAD_REQUEST).withBody(Json.stringify(body))
        )
    )

    val stream                     = Source.single(ByteString("<test></test>"))
    implicit val hc: HeaderCarrier = HeaderCarrier()

    whenReady(sut.postXml(MessageType.IE015, stream).value, timeout) {
      case Left(UpstreamErrorResponse(msg, 400, _, _)) => Json.parse(msg) mustBe body
      case Right(_)                                    => fail("Should not have succeeded")
      case _                                           => fail("A different error occurred")
    }

  }

  "On a failed connection, ensure we return a Left(Throwable)" in {
    val error              = new IllegalStateException
    val mockClient         = mock[HttpClientV2]
    val mockRequestBuilder = mock[RequestBuilder]
    when(mockClient.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
    when(
      mockRequestBuilder
        .withBody[RequestBuilder](any())(any(), any(), any())
    )
      .thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.stream(any(), any())).thenReturn(Future.failed(error))

    val failingSut = new ConversionConnectorImpl(appConfig, mockClient)
    val result     = failingSut.postXml(MessageType.IE015, Source.single(ByteString("")))(HeaderCarrier())

    whenReady(result.value, timeout) {
      case Left(e) if e == error => ()
      case Left(e)               => fail(s"A different error occurred: $e")
      case Right(_)              => fail("Should not have succeeded")
    }
  }

}
