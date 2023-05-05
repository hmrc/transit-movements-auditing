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

package uk.gov.hmrc.transitmovementsauditing.controllers.stream

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.implicits.catsSyntaxMonadError
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.streams.Accumulator
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.BaseControllerHelpers
import play.api.mvc.BodyParser
import play.api.mvc.Request
import play.api.mvc.Result

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait StreamingParsers {
  self: BaseControllerHelpers =>

  implicit val materializer: Materializer

  //  We have to be careful to not use Play's EC because we could accidentally starve the thread pool
  //  and cause errors for additional connections
  implicit val materializerExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  lazy val streamFromMemory: BodyParser[Source[ByteString, _]] = BodyParser {
    _ =>
      Accumulator.source[ByteString].map(Right.apply)
  }

  implicit class ActionBuilderStreamHelpers(actionBuilder: ActionBuilder[Request, _]) {

    /** Updates the [[Source]] with a version that can be used
      *  multiple times via the use of a temporary file.
      *
      *   @param block The code to use the with the reusable source
      *   @return An [[Action]]
      */
    // Implementation note: Tried to use the temporary file parser but it didn't pass the "single use" tests.
    // Doing it like this ensures that we can make sure that the source we pass is the file based one,
    // and only when it's ready.
    def streamFromFile(
      block: Request[Source[ByteString, _]] => Future[Result]
    )(implicit temporaryFileCreator: TemporaryFileCreator): Action[Source[ByteString, _]] =
      actionBuilder.async(streamFromMemory) {
        request =>
          val file = temporaryFileCreator.create()
          (for {
            _      <- request.body.runWith(FileIO.toPath(file))
            result <- block(request.withBody(FileIO.fromPath(file)))
          } yield result)
            .attemptTap {
              _ =>
                file.delete()
                Future.successful(())
            }
      }

  }

}
