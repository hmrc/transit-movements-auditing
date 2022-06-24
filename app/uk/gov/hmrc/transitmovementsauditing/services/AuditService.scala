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
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.transitmovementsauditing.models.AuditType
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.ArrivalNotification
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationAmendment
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationData
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationInvalidationRequest
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.PresentationNotificationForThePreLodgedDeclaration
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.RequestOfRelease
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.UnloadingRemarks
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Disabled
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Failure => AuditResultFailure}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Success => AuditResultSuccess}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {
  def send(auditType: AuditType, stream: Source[ByteString, _]): EitherT[Future, String, Unit]
}

@Singleton
class AuditServiceImpl @Inject() (connector: AuditConnector)(implicit ec: ExecutionContext, hc: HeaderCarrier, val materializer: Materializer)
    extends AuditService {

  def send(auditType: AuditType, stream: Source[ByteString, _]): EitherT[Future, String, Unit] =
    for {
      messageBody <- extractBody(stream)
      jsValue     <- parseJson(messageBody)
      extendedDataEvent = createExtendedEvent(auditType, jsValue)
      result <- sendEvent(extendedDataEvent)
    } yield result

  private def sendEvent(extendedDataEvent: ExtendedDataEvent): EitherT[Future, String, Unit] = {
    val futureResult = connector.sendExtendedEvent(extendedDataEvent)
    toEitherT(futureResult)
  }

  private def toEitherT(futureResult: Future[AuditResult]): EitherT[Future, String, Unit] =
    EitherT(
      futureResult
        .map {
          case AuditResultSuccess                => Right(())
          case AuditResultFailure(msg, None)     => Left(s"$msg")
          case AuditResultFailure(msg, Some(ex)) => Left(s"$msg, exception: $ex")
          case Disabled                          => Left("Disabled")
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

  private def extractBody(stream: Source[ByteString, _]): EitherT[Future, String, String] =
    EitherT {
      stream
        .fold("")(
          (cur, next) => cur + next.utf8String
        )
        .runWith(Sink.head[String])
        .map(Right(_))
        .recover {
          case NonFatal(ex) => Left("Error")
        }
    }

  private def parseJson(body: String): EitherT[Future, String, JsValue] =
    EitherT {
      Try(Json.parse(body)) match {
        case Success(jsonValue) => Future.successful(Right(jsonValue))
        case Failure(exception) => Future.successful(Left(exception.toString))
      }
    }

}
