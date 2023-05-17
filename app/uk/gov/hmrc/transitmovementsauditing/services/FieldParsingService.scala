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

import akka.stream.Materializer
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import com.google.inject.ImplementedBy
import com.google.inject.Singleton
import play.api.Logging
import uk.gov.hmrc.transitmovementsauditing.models.MessageType
import uk.gov.hmrc.transitmovementsauditing.models.errors.ParseError
import uk.gov.hmrc.transitmovementsauditing.services.XmlParsers.ParseResult
import uk.gov.hmrc.transitmovementsauditing.services.XmlParsers.concatKeyValue

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[FieldParsingServiceImpl])
trait FieldParsingService {

  def getAdditionalField(name: String, path: Seq[String], src: Source[ByteString, _]): Future[ParseResult[(String, String)]]

  def getAdditionalFields(messageType: Option[MessageType], src: Source[ByteString, _]): EitherT[Future, ParseError, Seq[ParseResult[(String, String)]]]
}

@Singleton
class FieldParsingServiceImpl(implicit ec: ExecutionContext, materializer: Materializer) extends FieldParsingService with ElementPaths with Logging {

  def getAdditionalField(name: String, path: Seq[String], src: Source[ByteString, _]): Future[ParseResult[(String, String)]] =
    src
      .via(XmlParsing.parser)
      .via(XmlParsers.extractElement(name, path))
      .runWith(concatKeyValue)

  def getAdditionalFields(messageType: Option[MessageType], src: Source[ByteString, _]): EitherT[Future, ParseError, Seq[ParseResult[(String, String)]]] =
    EitherT {
      messageType match {
        case Some(value) =>
          Future
            .sequence(
              elementPaths(value.messageCode).map {
                row =>
                  getAdditionalField(row._1, row._2, src) // node name and its path
              }.toSeq
            )
            .map(
              keyValuePairs => Right(keyValuePairs)
            )
        case None => Future.successful(Left(ParseError.NoElementFound(s"Unable to find $messageType")))
      }
    }
}
