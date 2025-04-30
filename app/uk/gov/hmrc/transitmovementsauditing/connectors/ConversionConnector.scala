/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementsauditing.connectors

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import cats.data.EitherT
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import io.lemonlabs.uri.UrlPath
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.libs.ws.DefaultBodyWritables
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.http.client.readStreamHttpResponse
import uk.gov.hmrc.transitmovementsauditing.models.MessageType

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

@ImplementedBy(classOf[ConversionConnectorImpl])
trait ConversionConnector {

  def postXml(messageType: MessageType, source: Source[ByteString, ?])(implicit hc: HeaderCarrier): EitherT[Future, Throwable, Source[ByteString, _]]

}

class ConversionConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(implicit materializer: Materializer, ec: ExecutionContext)
    extends ConversionConnector
    with DefaultBodyWritables
    with HttpErrorFunctions {

  private def converterPath(messageType: MessageType) =
    UrlPath.parse(s"/transit-movements-converter/messages/${messageType.messageCode}")

  override def postXml(
    messageType: MessageType,
    source: Source[ByteString, ?]
  )(implicit hc: HeaderCarrier): EitherT[Future, Throwable, Source[ByteString, ?]] =
    EitherT(
      httpClient
        .post(url"${appConfig.converterUrl.withPath(converterPath(messageType))}")
        .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.XML)
        .setHeader(HeaderNames.ACCEPT -> MimeTypes.JSON)
        .withBody(source)
        .stream[uk.gov.hmrc.http.HttpResponse]
        .flatMap {
          response =>
            if (is2xx(response.status)) Future.successful(Right(response.bodyAsSource))
            else
              response.bodyAsSource
                .reduce(_ ++ _)
                .map(_.utf8String)
                .runWith(Sink.head)
                .map(
                  body => Left(UpstreamErrorResponse(body, response.status))
                )
        }
        .recover {
          case NonFatal(e) => Left(e)
        }
    )
}
