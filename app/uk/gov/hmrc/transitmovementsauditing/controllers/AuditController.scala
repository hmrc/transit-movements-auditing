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
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import play.api.Logging
import play.api.http.MimeTypes
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.JsError
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
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
import uk.gov.hmrc.transitmovementsauditing.config.Constants.XAuditSourceHeader
import uk.gov.hmrc.transitmovementsauditing.controllers.actions.InternalAuthActionProvider
import uk.gov.hmrc.transitmovementsauditing.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsauditing.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.models.Details
import uk.gov.hmrc.transitmovementsauditing.models.FileId
import uk.gov.hmrc.transitmovementsauditing.models.Metadata
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
import scala.util.control.NonFatal

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

  def post(auditType: AuditType): Action[Source[ByteString, _]] =
    internalAuth(predicate).streamFromFile {
      implicit request =>
        if (auditType.messageType.isDefined) postMessageTypeAudit(auditType)
        else postStatusAudit(auditType)
    }

  private def postMessageTypeAudit(auditType: AuditType)(implicit request: Request[Source[ByteString, _]]): Future[Result] =
    if (appConfig.auditingEnabled) {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      (for {
        stream <- getSource(auditType, request)(exceedsMessageSize)

        details <- buildDetails(stream)
        result  <- auditService.sendMessageTypeEvent(auditType, details).asPresentation
      } yield result)
        .fold(
          presentationError => Status(presentationError.code.statusCode)(Json.toJson(presentationError)),
          _ => Accepted
        )
    } else {
      Future.successful(Accepted)
    }

  private def buildDetails(payload: Payload)(implicit request: Request[Source[ByteString, _]]): EitherT[Future, PresentationError, Details] =
    if (request.headers.get(Constants.XAuditMetaPath).isEmpty) {
      EitherT.leftT[Future, Details](PresentationError.badRequestError(s"${Constants.XAuditMetaPath} is missing"))
    } else {
      val metadata = Metadata(request.headers)
      payload match {
        case Left(summary) =>
          val objSummary = Json.obj(
            "objectSummary"    -> summary.objectSummary.toString,
            "additionalFields" -> summary.fields.toString()
          )
          EitherT.rightT[Future, PresentationError](Details(metadata, Some(objSummary)))
        case Right(s) =>
          for {
            body <- extractBody(s)
            src  <- parseObj(body)
          } yield Details(metadata, Some(src))
      }
    }

  def postStatusAudit(auditType: AuditType)(implicit request: Request[Source[ByteString, _]]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    val auditSource = request.headers.get(XAuditSourceHeader).getOrElse(auditType.source)

    (for {
      string  <- extractBody(request.body)
      details <- parseDetails(string)
      result  <- auditService.sendStatusTypeEvent(details, auditType.name, auditSource).asPresentation
    } yield result)
      .fold(
        presentationError => Status(presentationError.code.statusCode)(Json.toJson(presentationError)),
        _ => Accepted
      )
  }

  //TODO: combine parseObj and parseDetails??
  //      e.g. private def parseXX[A](body: String): EitherT[Future, PresentationError, A]= ???
  //      requires an implicit reads for the validate!

  private def parseObj(body: String): EitherT[Future, PresentationError, JsObject] =
    Json
      .parse(body)
      .validate[JsObject]
      .map(
        x => EitherT.rightT[Future, PresentationError](x)
      )
      .recoverTotal {
        err: JsError => EitherT.leftT(PresentationError.badRequestError(s"Could not parse: $err"))
      }

  private def parseDetails(body: String): EitherT[Future, PresentationError, Details] =
    Json
      .parse(body)
      .validate[Details]
      .map(
        x => EitherT.rightT[Future, PresentationError](x)
      )
      .recoverTotal {
        err: JsError => EitherT.leftT(PresentationError.badRequestError(s"Could not parse: $err"))
      }

  private def extractBody(stream: Source[ByteString, _]): EitherT[Future, PresentationError, String] =
    EitherT {
      stream
        .reduce(_ ++ _)
        .map(_.utf8String)
        .runWith(Sink.head)
        .map(Right.apply)
        .recover {
          case NonFatal(ex) => Left(PresentationError.internalServiceError(cause = Some(ex)))
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
    else {
      EitherT.rightT(request.body)
    }

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
