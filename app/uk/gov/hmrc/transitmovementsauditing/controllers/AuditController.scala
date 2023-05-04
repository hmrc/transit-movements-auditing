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
import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.transitmovementsauditing.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsauditing.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.models.FileId
import uk.gov.hmrc.transitmovementsauditing.models.ObjectStoreResourceLocation
import uk.gov.hmrc.transitmovementsauditing.models.ObjectSummaryWithFields
import uk.gov.hmrc.transitmovementsauditing.models.errors.ConversionError
import uk.gov.hmrc.transitmovementsauditing.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsauditing.Payload
import uk.gov.hmrc.transitmovementsauditing.services.AuditService
import uk.gov.hmrc.transitmovementsauditing.services.ConversionService
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
  appConfig: AppConfig
)(implicit
  val materializer: Materializer
) extends BackendController(cc)
    with StreamingParsers
    with ErrorTranslator
    with Logging {

  def post(auditType: AuditType, uri: Option[ObjectStoreResourceLocation] = None): Action[Source[ByteString, _]] = Action.async(streamFromMemory) {

    implicit request =>
      if (appConfig.auditingEnabled) {
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

        (for {
          stream <- getSource(auditType, uri, request)(exceedsMessageSize)
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
      .map(_.toLong > appConfig.auditMessageMaxSize)
      .getOrElse(false)

  def postLarge(auditType: AuditType, uri: String): Action[Source[ByteString, _]] =
    post(auditType, Some(ObjectStoreResourceLocation(uri).stripRoutePrefix))

  private def convertIfNecessary(auditType: AuditType, request: Request[Source[ByteString, _]])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ConversionError, Source[ByteString, _]] =
    if (request.contentType.contains(MimeTypes.XML) && auditType.messageType.isDefined)
      conversionService.toJson(auditType.messageType.get, request.body)
    else
      EitherT.rightT(request.body)

  private def fileId(): FileId =
    FileId(s"${UUID.randomUUID().toString}-${DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss.SSS").withZone(ZoneOffset.UTC).format(Instant.now())}")

  private def getSource(auditType: AuditType, uri: Option[ObjectStoreResourceLocation], request: Request[Source[ByteString, _]])(exceedsLimit: Boolean)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, PresentationError, Payload] =
    (uri, exceedsLimit) match {
      case (None, false) =>
        logger.info("Payload in body and < auditing message limit")
        convertIfNecessary(auditType, request).asPresentation
          .map(Right(_))
      case (Some(uri), false) =>
        logger.info("Payload in object store and < auditing message limit")
        val contents = objectStoreService
          .getContents(uri)
          .asPresentation
        contents.map(Right(_))
      case (None, true) =>
        logger.info("Payload in body and > auditing message limit")
        (for {
          parseResults <- auditService.getAdditionalFields(auditType.messageType, request.body).asPresentation
          keyValuePairs = parseResults.collect {
            case Right(pair) => pair
          }
          objSummary <- objectStoreService.putFile(fileId(), request.body).asPresentation
        } yield ObjectSummaryWithFields(objSummary, keyValuePairs)).map {
          summaryWithFields => Left(summaryWithFields)
        }
      case (Some(uri), true) =>
        logger.info("Payload in object store and > auditing message limit")
        for {
          contents     <- objectStoreService.getContents(uri).asPresentation
          parseResults <- auditService.getAdditionalFields(auditType.messageType, request.body).asPresentation
          keyValuePairs = parseResults.collect {
            case Right(pair) => pair
          }
          objSummary <- objectStoreService.putFile(fileId(), contents).asPresentation
        } yield Left(ObjectSummaryWithFields(objSummary, keyValuePairs))
    }

}
