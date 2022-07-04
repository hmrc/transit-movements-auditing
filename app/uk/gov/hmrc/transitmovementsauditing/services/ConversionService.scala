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

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import com.google.inject.ImplementedBy
import uk.gov.hmrc.transitmovementsauditing.models.errors.ConversionError

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[ConversionServiceImpl])
trait ConversionService {

  def toJson(xmlStream: Source[ByteString, _])(implicit mat: Materializer, ec: ExecutionContext): EitherT[Future, ConversionError, Source[ByteString, _]]

}

class ConversionServiceImpl extends ConversionService {

  // TODO: Dummy implementation for now, awaiting conversion service to be ready
  override def toJson(
    xmlStream: Source[ByteString, _]
  )(implicit mat: Materializer, ec: ExecutionContext): EitherT[Future, ConversionError, Source[ByteString, _]] = {
    xmlStream.run() // right now, we don't care about this but we need to drain it.
    EitherT.rightT(Source.single(ByteString("{ \"dummy\": \"dummy\" }", StandardCharsets.UTF_8)))
  }

}
