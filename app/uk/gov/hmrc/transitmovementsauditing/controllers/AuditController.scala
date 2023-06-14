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

package uk.gov.hmrc.transitmovementsauditing.controllers

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import play.api.Logging
import play.api.http.MimeTypes
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.IAAction
import uk.gov.hmrc.internalauth.client.Predicate
import uk.gov.hmrc.internalauth.client.Resource
import uk.gov.hmrc.internalauth.client.ResourceLocation
import uk.gov.hmrc.internalauth.client.ResourceType
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.transitmovementsauditing.Payload
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.transitmovementsauditing.controllers.actions.InternalAuthActionProvider
import uk.gov.hmrc.transitmovementsauditing.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsauditing.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.models.FileId
import uk.gov.hmrc.transitmovementsauditing.models.ObjectSummaryWithFields
import uk.gov.hmrc.transitmovementsauditing.models.errors.ConversionError
import uk.gov.hmrc.transitmovementsauditing.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsauditing.services.AuditService
import uk.gov.hmrc.transitmovementsauditing.services.ConversionService
import uk.gov.hmrc.transitmovementsauditing.services.FieldParsingService
import uk.gov.hmrc.transitmovementsauditing.services.ObjectStoreService

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton()
class AuditController @Inject() (
  cc: ControllerComponents,
  conversionService: ConversionService,
  auditService: AuditService,
  objectStoreService: ObjectStoreService,
  fieldParsingService: FieldParsingService,
  internalAuth: InternalAuthActionProvider,
  appConfig: AppConfig
)(implicit
  val materializer: Materializer,
  val temporaryFileCreator: TemporaryFileCreator
) extends BackendController(cc)
    with StreamingParsers
    with ErrorTranslator
    with Logging {

  private val predicate = Predicate.Permission(Resource(ResourceType("transit-movements-auditing"), ResourceLocation("audit")), IAAction("WRITE"))

  def post(auditType: AuditType): Action[Source[ByteString, _]] = internalAuth(predicate).streamFromFile {

    implicit request =>
      if (appConfig.auditingEnabled) {
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

        (for {
          stream <- getSource(auditType, request)(exceedsMessageSize)
          result <- auditService.send(auditType, stream).asPresentation
        } yield result)
          .fold(
            presentationError => Status(presentationError.code.statusCode)(Json.toJson(presentationError)),
            _ => Accepted
          )
      } else {
        Future.successful(Accepted)
      }
  }

  private def exceedsMessageSize(implicit request: Request[Source[ByteString, _]]): Boolean =
    request.headers
      .get(Constants.XContentLengthHeader)
      .exists(_.toLong > appConfig.auditMessageMaxSize)

  private def convertIfNecessary(auditType: AuditType, request: Request[Source[ByteString, _]])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ConversionError, Source[ByteString, _]] =
    if (request.contentType.contains(MimeTypes.XML) && auditType.messageType.isDefined)
      conversionService.toJson(auditType.messageType.get, request.body)
    else
      EitherT.rightT(request.body)

  private def fileId(): FileId =
    FileId(s"${UUID.randomUUID().toString}-${DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss.SSS").withZone(ZoneOffset.UTC).format(Instant.now())}")

  private def getSource(auditType: AuditType, request: Request[Source[ByteString, _]])(exceedsLimit: Boolean)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, PresentationError, Payload] =
    if (exceedsLimit) {
      logger.info("Payload in body and > auditing message limit")
      (for {
        parseResults <- fieldParsingService.getAdditionalFields(auditType.messageType, request.body).asPresentation
        keyValuePairs = parseResults.collect {
          case Right(pair) => pair
        }
        objSummary <- objectStoreService.putFile(fileId(), request.body).asPresentation
      } yield ObjectSummaryWithFields(objSummary, keyValuePairs)).map {
        summaryWithFields => Left(summaryWithFields)
      }
    } else {
      logger.info("Payload in body and < auditing message limit")
      convertIfNecessary(auditType, request).asPresentation
        .map(Right(_))
    }

}
