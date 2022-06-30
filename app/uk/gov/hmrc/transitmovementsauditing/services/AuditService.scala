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
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import com.fasterxml.jackson.core.JsonParseException
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Disabled
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Failure => AuditResultFailure}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Success => AuditResultSuccess}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.transitmovementsauditing.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.ArrivalNotification
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationAmendment
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationData
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationInvalidationRequest
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.PresentationNotificationForThePreLodgedDeclaration
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.RequestOfRelease
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.UnloadingRemarks
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {
  def send(auditType: AuditType, jsonStream: Source[ByteString, _]): EitherT[Future, AuditError, Unit]
}

@Singleton
class AuditServiceImpl @Inject() (connector: AuditConnector)(implicit ec: ExecutionContext, hc: HeaderCarrier, val materializer: Materializer)
    extends AuditService {

  def send(auditType: AuditType, jsonStream: Source[ByteString, _]): EitherT[Future, AuditError, Unit] =
    for {
      messageBody <- extractBody(jsonStream)
      jsValue     <- parseJson(messageBody)
      extendedDataEvent = createExtendedEvent(auditType, jsValue)
      result <- sendEvent(extendedDataEvent)
    } yield result

  private def sendEvent(extendedDataEvent: ExtendedDataEvent): EitherT[Future, AuditError, Unit] = {
    val futureResult = connector.sendExtendedEvent(extendedDataEvent)
    toEitherT(futureResult)
  }

  private def toEitherT(futureResult: Future[AuditResult]): EitherT[Future, AuditError, Unit] =
    EitherT(
      futureResult
        .map {
          case AuditResultSuccess           => Right(())
          case AuditResultFailure(msg, thr) => Left(AuditError.UnexpectedError(msg, thr))
          case Disabled                     => Left(AuditError.Disabled)
        }
    )

  private def createExtendedEvent(auditType: AuditType, messageBody: JsValue): ExtendedDataEvent =
    ExtendedDataEvent(
      auditSource = source(auditType),
      auditType = auditType.name,
      tags = hc.toAuditTags(),
      detail = messageBody
    )

  private def source(auditType: AuditType): String =
    auditType match {
      case ArrivalNotification | DeclarationAmendment | DeclarationInvalidationRequest | DeclarationData | UnloadingRemarks | RequestOfRelease |
          PresentationNotificationForThePreLodgedDeclaration =>
        "common-transit-convention-traders"
      case _ =>
        "transit-movements-router"
    }

  private def extractBody(stream: Source[ByteString, _]): EitherT[Future, AuditError, String] =
    EitherT {
      stream
        .fold(ByteString())(
          (cur, next) => cur ++ next
        )
        .map(_.utf8String)
        .runWith(Sink.head[String])
        .map(Right(_))
        .recover {
          case NonFatal(ex) => Left(AuditError.UnexpectedError(s"Error extracting body from stream", Some(ex)))
        }
    }

  private def parseJson(body: String): EitherT[Future, AuditError, JsValue] =
    EitherT {
      Try(Json.parse(body)) match {
        case Success(jsonValue)                     => Future.successful(Right(jsonValue))
        case Failure(exception: JsonParseException) => Future.successful(Left(AuditError.FailedToParse(exception)))
        case Failure(exception) =>
          Future.successful(Left(AuditError.UnexpectedError("Exception thrown when attempting to parse string as Json", Some(exception))))
      }
    }

}
