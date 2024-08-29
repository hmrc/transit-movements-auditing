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

package uk.gov.hmrc.transitmovementsauditing.v2_1.services

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.xml.ParseEvent
import org.apache.pekko.stream.connectors.xml.scaladsl.XmlParsing
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Sink

import scala.concurrent.Future

object XmlParsers extends XmlParsingServiceHelpers {

  def extractElement(name: String, path: Seq[String]): Flow[ParseEvent, ParseResult[(String, String)], NotUsed] =
    XmlParsing
      .subtree(path) // path
      .collect {
        case element if element.getTextContent.nonEmpty => element.getTextContent
      }
      .single(name)

  val concatKeyValue: Sink[ParseResult[(String, String)], Future[ParseResult[(String, String)]]] = Sink.reduce[ParseResult[(String, String)]](
    (k, v) =>
      k.flatMap(
        key =>
          v.map {
            value => (key._1, value._1) // ??
          }
      )
  )

}
