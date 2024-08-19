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

import cats.data.EitherT
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.Logging
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
import uk.gov.hmrc.transitmovementsauditing.models.Details
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {

  def sendMessageTypeEvent(auditType: AuditType, details: Details)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit]

  def sendStatusTypeEvent(details: Details, auditName: String, auditSource: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit]

}

@Singleton
class AuditServiceImpl @Inject() (connector: AuditConnector)(implicit ec: ExecutionContext) extends AuditService with Logging {

  def sendMessageTypeEvent(auditType: AuditType, details: Details)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit] = {
    val extendedDataEvent = createExtendedEvent(auditType, Json.toJson(details))
    for {
      result <- sendEvent(extendedDataEvent)
    } yield result
  }

  def sendStatusTypeEvent(details: Details, auditName: String, auditSource: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit] =
    for {
      result <- sendEvent(createExtendedEventForStatusAudit(auditName, auditSource, details))
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

  private def createExtendedEvent(auditType: AuditType, messageBody: JsValue)(implicit hc: HeaderCarrier): ExtendedDataEvent =
    ExtendedDataEvent(
      auditSource = auditType.source,
      auditType = auditType.name,
      tags = hc.toAuditTags(),
      detail = messageBody
    )

  private def createExtendedEventForStatusAudit(auditName: String, auditSource: String, messageBody: Details)(implicit
    hc: HeaderCarrier
  ): ExtendedDataEvent =
    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditName,
      tags = hc.toAuditTags(),
      detail = Json.toJson(messageBody)
    )
}
