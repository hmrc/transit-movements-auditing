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

package uk.gov.hmrc.transitmovementsauditing.controllers

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import uk.gov.hmrc.transitmovementsauditing.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsauditing.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsauditing.services.AuditService
import uk.gov.hmrc.transitmovementsauditing.services.ConversionService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton()
class AuditController @Inject() (cc: ControllerComponents, conversionService: ConversionService, auditService: AuditService)(implicit
  val materializer: Materializer
) extends BackendController(cc)
    with StreamingParsers
    with ErrorTranslator {

  def post(auditType: AuditType): Action[Source[ByteString, _]] = Action.async(streamFromMemory) {
    request =>
      (for {
        jsonStream <- convertIfNecessary(request)
        result     <- auditService.send(auditType, jsonStream).asPresentation
      } yield result)
        .fold(
          presentationError => Status(presentationError.code.statusCode)(Json.toJson(presentationError)),
          _ => Accepted
        )
  }

  private def convertIfNecessary(request: Request[Source[ByteString, _]]): EitherT[Future, PresentationError, Source[ByteString, _]] =
    // TODO: is this okay to check for an XML content type?
    if (
      request.contentType.exists(
        contentType => contentType.contains("/xml")
      )
    ) conversionService.toJson(request.body).asPresentation
    else EitherT.rightT(request.body)
}
