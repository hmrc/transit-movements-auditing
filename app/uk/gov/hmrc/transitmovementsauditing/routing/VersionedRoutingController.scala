/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementsauditing.routing

import cats.implicits.catsSyntaxEitherId
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.transitmovementsauditing.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementsauditing.models.{AuditType => TransitionalAuditType}
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.{AuditType => FinalAuditType}
import uk.gov.hmrc.transitmovementsauditing.controllers.{AuditController => TransitionalAuditController}
import uk.gov.hmrc.transitmovementsauditing.v2_1.controllers.{AuditController => FinalAuditController}

import javax.inject.Inject
import scala.concurrent.Future

class VersionedRoutingController @Inject() (
  cc: ControllerComponents,
  transitionalController: TransitionalAuditController,
  finalController: FinalAuditController
)(implicit val materializer: Materializer)
    extends BackendController(cc)
    with StreamingParsers {

  def post(auditType: String): Action[Source[ByteString, _]] =
    Action.async(streamFromMemory) {
      implicit request =>
        request.headers.get(Constants.APIVersionHeaderKey).map(_.trim.toLowerCase) match {
          case Some(Constants.APIVersionFinalHeaderValue) =>
            validateFinalAuditType(auditType)
              .flatMap(
                aType => finalController.post(aType)(request).asRight
              )
              .merge
          case _ =>
            validateTransitionalAuditType(auditType)
              .flatMap(
                aType => transitionalController.post(aType)(request).asRight
              )
              .merge
        }
    }

  private def validateFinalAuditType(auditType: String): Either[Future[Result], FinalAuditType] =
    FinalAuditType.values.find(_.name == auditType) match {
      case Some(value) => value.asRight
      case None        => Future.successful(BadRequest(s"Invalid audit type: $auditType")).asLeft
    }

  private def validateTransitionalAuditType(auditType: String): Either[Future[Result], TransitionalAuditType] =
    TransitionalAuditType.values.find(_.name == auditType) match {
      case Some(value) => value.asRight
      case None        => Future.successful(BadRequest(s"Invalid audit type: $auditType")).asLeft
    }
}
