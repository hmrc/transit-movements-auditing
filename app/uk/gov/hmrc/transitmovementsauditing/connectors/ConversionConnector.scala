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

package uk.gov.hmrc.transitmovementsauditing.connectors

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import io.lemonlabs.uri.UrlPath
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.models.MessageType

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[ConversionConnectorImpl])
trait ConversionConnector {

  def postXml(messageType: MessageType, source: Source[ByteString, _])(implicit hc: HeaderCarrier): Future[Source[ByteString, _]]

}

class ConversionConnectorImpl @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit materializer: Materializer, ec: ExecutionContext)
    extends ConversionConnector {

  lazy val converterPath = UrlPath.parse(s"${appConfig.converterUrl}/transit-movements-converter/convert")

  override def postXml(messageType: MessageType, source: Source[ByteString, _])(implicit hc: HeaderCarrier): Future[Source[ByteString, _]] =
    httpClient
      .post(url"$converterPath/${messageType.value}")
      .replaceHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.XML)
      .withBody(source)
      .stream[Source[ByteString, _]]

}
