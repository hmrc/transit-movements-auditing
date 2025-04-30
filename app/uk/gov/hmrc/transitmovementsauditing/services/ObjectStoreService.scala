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

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import cats.data.EitherT
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import play.api.Logging
import play.api.http.MimeTypes
import uk.gov.hmrc.http.HeaderCarrier
import org.apache.pekko.stream.Materializer

import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.models.FileId
import uk.gov.hmrc.transitmovementsauditing.models.errors.ObjectStoreError

@ImplementedBy(classOf[ObjectStoreServiceImpl])
trait ObjectStoreService {

  def putFile(fileId: FileId, source: Source[ByteString, ?])(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, ObjectStoreError, ObjectSummaryWithMd5]
}

@Singleton
class ObjectStoreServiceImpl @Inject() (appConfig: AppConfig)(implicit materializer: Materializer, client: PlayObjectStoreClient)
    extends ObjectStoreService
    with Logging {

  def putFile(fileId: FileId, source: Source[ByteString, ?])(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, ObjectStoreError, ObjectSummaryWithMd5] = EitherT {

    client
      .putObject(
        path = Path.Directory("auditing").file(s"${fileId.value}"),
        content = source,
        owner = appConfig.appName,
        contentType = Some(MimeTypes.XML)
      )
      .map {
        objectSummary =>
          Right(objectSummary)
      }
      .recover {
        case NonFatal(thr) =>
          Left(ObjectStoreError.UnexpectedError(Some(thr)))
      }
  }

}
