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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.transitmovementsauditing.connectors.ConversionConnector
import uk.gov.hmrc.transitmovementsauditing.models.MessageType
import uk.gov.hmrc.transitmovementsauditing.models.errors.ConversionError
import uk.gov.hmrc.transitmovementsauditing.models.errors.StandardError

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

@ImplementedBy(classOf[ConversionServiceImpl])
trait ConversionService {

  def toJson(messageType: MessageType, xmlStream: Source[ByteString, _])(implicit hc: HeaderCarrier): EitherT[Future, ConversionError, Source[ByteString, _]]

}

class ConversionServiceImpl @Inject() (conversionConnector: ConversionConnector)(implicit ec: ExecutionContext) extends ConversionService {

  override def toJson(
    messageType: MessageType,
    xmlStream: Source[ByteString, _]
  )(implicit hc: HeaderCarrier): EitherT[Future, ConversionError, Source[ByteString, _]] =
    conversionConnector
      .postXml(messageType, xmlStream)
      .leftMap {
        case UpstreamErrorResponse(m, 400, _, _) => ConversionError.FailedConversion(Json.parse(m).as[StandardError].message)
        case NonFatal(e)                         => ConversionError.UnexpectedError("An error was returned when converting the XML to Json", thr = Some(e))
      }

}
