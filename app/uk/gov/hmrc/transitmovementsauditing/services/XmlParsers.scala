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

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.alpakka.xml.ParseEvent
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink

import scala.concurrent.Future

object XmlParsers extends XmlParsingServiceHelpers {

  def extractElement(name: String, path: Seq[String])(implicit mat: Materializer): Flow[ParseEvent, ParseResult[String], NotUsed] =
    XmlParsing
      .subtree(path) // path
      .collect {
        case element if element.getTextContent.nonEmpty => element.getTextContent
      }
      .single(name)

  val concatKeyValue: Sink[ParseResult[String], Future[ParseResult[String]]] = Sink.reduce[ParseResult[String]](
    (k, v) =>
      k.flatMap(
        key =>
          v.map {
            value => s"($key,$value)"
          }
      )
  )

}
