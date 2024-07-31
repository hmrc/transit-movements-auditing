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

import cats.data.EitherT
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.http.MimeTypes
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.JsError
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.transitmovementsauditing.Payload
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.transitmovementsauditing.config.Constants.XAuditSourceHeader
import uk.gov.hmrc.transitmovementsauditing.controllers.actions.InternalAuthActionProvider
import uk.gov.hmrc.transitmovementsauditing.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsauditing.models._
import uk.gov.hmrc.transitmovementsauditing.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsauditing.models.request.DetailsRequest
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
    internalAuth(predicate).async(streamFromMemory) {
      implicit request =>
        if (auditType.messageType.isDefined) postMessageTypeAudit(auditType)
        else postStatusAudit(auditType)
    }

  private def postMessageTypeAudit(auditType: AuditType)(implicit request: Request[Source[ByteString, _]]): Future[Result] =
    if (appConfig.auditingEnabled) {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      (for {
        stream  <- getSource(auditType, request)(exceedsMessageSize)
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

  private def buildDetails(payload: Payload)(implicit
    request: Request[Source[ByteString, _]]
  ): EitherT[Future, PresentationError, Details] =
    if (request.headers.get(Constants.XAuditMetaPath).isEmpty) {
      EitherT.leftT[Future, Details](PresentationError.badRequestError(s"${Constants.XAuditMetaPath} is missing"))
    } else {
      val (clientId, channel) = getChannelAndClientId(request.headers)

      val metadata = Metadata(
        request.headers.get(Constants.XAuditMetaPath).get,
        request.headers.get(Constants.XAuditMetaMovementId).map(MovementId(_)),
        request.headers.get(Constants.XAuditMetaMessageId).map(MessageId(_)),
        request.headers.get(Constants.XAuditMetaEORI).map(EORINumber(_)),
        request.headers.get(Constants.XAuditMetaMovementType).flatMap(MovementType.findByName(_)),
        request.headers.get(Constants.XAuditMetaMessageType).flatMap(MessageType.findByCode(_)),
        clientId,
        channel,
        request.headers.get(Constants.XContentLengthHeader).map(_.toLong)
      )

      payload match {
        case Left(summary) =>
          val objSummary = Json.obj(
            "objectSummary"    -> summary.objectSummary.toString,
            "additionalFields" -> summary.fields.toString()
          )
          EitherT.rightT[Future, PresentationError](
            Details(
              None,
              metadata,
              Some(objSummary)
            )
          )
        case Right(s) =>
          for {
            body <- extractBody(s)
            src  <- parse[JsObject](body)
          } yield Details(
            None,
            metadata,
            Some(src)
          )
      }
    }

  private def getChannelAndClientId(headers: Headers, auditType: Option[AuditType] = None) = {
    val clientId = headers.get(Constants.XClientIdHeader).map(ClientId(_))
    val channel  = if (auditType.isDefined && (auditType.get == AuditType.NCTSRequestedMissingMovement)) None else Channel.getChannel(clientId)
    (clientId, channel)
  }

  def postStatusAudit(auditType: AuditType)(implicit request: Request[Source[ByteString, _]]): Future[Result] =
    if (appConfig.auditingEnabled) {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val auditSource = request.headers.get(XAuditSourceHeader).getOrElse(auditType.source)

      val (clientId, channel) = getChannelAndClientId(request.headers, Some(auditType))

      (for {
        string         <- extractBody(request.body)
        detailsRequest <- parse[DetailsRequest](string)
        subType = if (auditType.parent.isDefined) Some(auditType.name) else None
        result <- auditService
          .sendStatusTypeEvent(
            Details(
              subType,
              Metadata(
                detailsRequest.metadata.path,
                detailsRequest.metadata.movementId,
                detailsRequest.metadata.messageId,
                detailsRequest.metadata.enrolmentEORI,
                detailsRequest.metadata.movementType,
                detailsRequest.metadata.messageType,
                clientId,
                channel
              ),
              detailsRequest.payload
            ),
            auditType.parent.getOrElse(auditType.name).toString,
            auditSource
          )
          .asPresentation
      } yield result)
        .fold(
          presentationError => Status(presentationError.code.statusCode)(Json.toJson(presentationError)),
          _ => Accepted
        )
    } else {
      Future.successful(Accepted)
    }

  private def parse[A: Reads](body: String): EitherT[Future, PresentationError, A] =
    Json
      .parse(body)
      .validate[A]
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
  ): EitherT[Future, PresentationError, Source[ByteString, _]] =
    for {
      sources <- reUsableSource(request, 2)
      converted <-
        if (request.contentType.contains(MimeTypes.XML) && auditType.messageType.isDefined)
          conversionService.toJson(auditType.messageType.get, sources.head).asPresentation
        else
          EitherT.rightT[Future, PresentationError](sources.lift(1).get)
    } yield converted

  private def fileId(): FileId =
    FileId(s"${UUID.randomUUID().toString}-${DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss.SSS").withZone(ZoneOffset.UTC).format(Instant.now())}")

  private def getSource(auditType: AuditType, request: Request[Source[ByteString, _]])(exceedsLimit: Boolean)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, PresentationError, Payload] =
    if (exceedsLimit) {
      logger.info("Payload in body and > auditing message limit")
      (for {
        sources      <- reUsableSource(request, 2)
        parseResults <- fieldParsingService.getAdditionalFields(auditType.messageType, sources.head).asPresentation
        keyValuePairs = parseResults.collect {
          case Right(pair) => pair
        }
        objSummary <- objectStoreService.putFile(fileId(), sources.lift(1).get).asPresentation
      } yield ObjectSummaryWithFields(objSummary, keyValuePairs)).map {
        summaryWithFields => Left(summaryWithFields)
      }
    } else {
      logger.info("Payload in body and < auditing message limit")
      convertIfNecessary(auditType, request)
        .map(Right(_))
    }

  private def materializeSource(source: Source[ByteString, _]): EitherT[Future, PresentationError, Seq[ByteString]] =
    EitherT(
      source
        .runWith(Sink.seq)
        .map(Right(_): Either[PresentationError, Seq[ByteString]])
        .recover {
          error =>
            Left(PresentationError.internalServiceError(cause = Some(error)))
        }
    )

  // Function to create a new source from the materialized sequence
  private def createReusableSource(seq: Seq[ByteString]): Source[ByteString, _] = Source(seq.toList)

  private def reUsableSource(
    request: Request[Source[ByteString, _]],
    numberOfSources: Int = 3
  ): EitherT[Future, PresentationError, List[Source[ByteString, _]]] = for {
    byteStringSeq <- materializeSource(request.body)
  } yield List.fill(numberOfSources)(createReusableSource(byteStringSeq))

}
