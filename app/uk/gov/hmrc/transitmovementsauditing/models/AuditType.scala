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

package uk.gov.hmrc.transitmovementsauditing.models

import Sources._

sealed abstract class AuditType(val name: String, val source: String, val messageType: Option[MessageType] = None) extends Product with Serializable

object Sources {
  val commonTransitConventionTraders = "common-transit-convention-traders"
  val transitMovementsRouter         = "transit-movements-router"
}

object AuditType {

  val values: Seq[AuditType] = Seq(
    AmendmentAcceptance,
    ArrivalNotification,
    InvalidationDecision,
    DeclarationAmendment,
    DeclarationInvalidationRequest,
    DeclarationData,
    Discrepancies,
    NotificationToAmendDeclaration,
    GoodsReleasedNotification,
    MRNAllocated,
    ReleaseForTransit,
    RecoveryNotification,
    UnloadingPermission,
    UnloadingRemarks,
    InformationAboutNonArrivedMovement,
    WriteOffNotification,
    NoReleaseForTransit,
    RequestOfRelease,
    GuaranteeNotValid,
    RejectionFromOfficeOfDeparture,
    RejectionFromOfficeOfDestination,
    PresentationNotificationForThePreLodgedDeclaration,
    ControlDecisionNotification,
    ForwardedIncidentNotificationToED,
    PositiveAcknowledge,
    LargeMessageSubmissionRequested
  )

  case object AmendmentAcceptance            extends AuditType("AmendmentAcceptance", transitMovementsRouter, Some(MessageType.IE004))
  case object ArrivalNotification            extends AuditType("ArrivalNotification", commonTransitConventionTraders, Some(MessageType.IE007))
  case object InvalidationDecision           extends AuditType("InvalidationDecision", transitMovementsRouter, Some(MessageType.IE009))
  case object DeclarationAmendment           extends AuditType("DeclarationAmendment", commonTransitConventionTraders, Some(MessageType.IE013))
  case object DeclarationInvalidationRequest extends AuditType("DeclarationInvalidationRequest", commonTransitConventionTraders, Some(MessageType.IE014))
  case object DeclarationData                extends AuditType("DeclarationData", commonTransitConventionTraders, Some(MessageType.IE015))
  case object Discrepancies                  extends AuditType("Discrepancies", transitMovementsRouter, Some(MessageType.IE019))
  case object NotificationToAmendDeclaration extends AuditType("NotificationToAmendDeclaration", transitMovementsRouter, Some(MessageType.IE022))
  case object GoodsReleasedNotification      extends AuditType("GoodsReleasedNotification", transitMovementsRouter, Some(MessageType.IE025))
  case object MRNAllocated                   extends AuditType("MRNAllocated", transitMovementsRouter, Some(MessageType.IE028))
  case object ReleaseForTransit              extends AuditType("ReleaseForTransit", transitMovementsRouter, Some(MessageType.IE029))
  case object RecoveryNotification           extends AuditType("RecoveryNotification", transitMovementsRouter, Some(MessageType.IE035))
  case object UnloadingPermission            extends AuditType("UnloadingPermission", transitMovementsRouter, Some(MessageType.IE043))
  case object UnloadingRemarks               extends AuditType("UnloadingRemarks", commonTransitConventionTraders, Some(MessageType.IE044))

  case object InformationAboutNonArrivedMovement
      extends AuditType("InformationAboutNonArrivedMovement", commonTransitConventionTraders, Some(MessageType.IE141))
  case object WriteOffNotification              extends AuditType("WriteOffNotification", transitMovementsRouter, Some(MessageType.IE045))
  case object NoReleaseForTransit               extends AuditType("NoReleaseForTransit", transitMovementsRouter, Some(MessageType.IE051))
  case object RequestOfRelease                  extends AuditType("RequestOfRelease", commonTransitConventionTraders, Some(MessageType.IE054))
  case object GuaranteeNotValid                 extends AuditType("GuaranteeNotValid", transitMovementsRouter, Some(MessageType.IE055))
  case object RejectionFromOfficeOfDeparture    extends AuditType("RejectionFromOfficeOfDeparture", transitMovementsRouter, Some(MessageType.IE056))
  case object RejectionFromOfficeOfDestination  extends AuditType("RejectionFromOfficeOfDestination", transitMovementsRouter, Some(MessageType.IE057))
  case object ControlDecisionNotification       extends AuditType("ControlDecisionNotification", transitMovementsRouter, Some(MessageType.IE060))
  case object ForwardedIncidentNotificationToED extends AuditType("ForwardedIncidentNotificationToED", commonTransitConventionTraders, Some(MessageType.IE182))
  case object PositiveAcknowledge               extends AuditType("PositiveAcknowledge", transitMovementsRouter, Some(MessageType.IE928))
  case object RequestOnNonArrivedMovement       extends AuditType("RequestOnNonArrivedMovement", transitMovementsRouter, Some(MessageType.IE140))
  case object LargeMessageSubmissionRequested   extends AuditType("LargeMessageSubmissionRequested", commonTransitConventionTraders, None)

  case object PresentationNotificationForThePreLodgedDeclaration
      extends AuditType("PresentationNotificationForThePreLodgedDeclaration", commonTransitConventionTraders, Some(MessageType.IE170))

  def fromName(name: String): Option[AuditType] = values.find(_.name == name)

}
