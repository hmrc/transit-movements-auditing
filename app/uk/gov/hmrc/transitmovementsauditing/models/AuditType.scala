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
import uk.gov.hmrc.transitmovementsauditing.models.ParentAuditType.CTCTradersFailed
import uk.gov.hmrc.transitmovementsauditing.models.ParentAuditType.CTCTradersSucceeded
import uk.gov.hmrc.transitmovementsauditing.models.ParentAuditType.CTCTradersWorkflow

sealed abstract class AuditType(val name: String, val source: String, val messageType: Option[MessageType] = None, val parent: Option[ParentAuditType])
    extends Product
    with Serializable

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
    TraderFailedUploadEvent,
    SubmitArrivalNotificationFailedEvent,
    SubmitDeclarationFailedEvent,
    ValidationFailedEvent,
    CreateMovementDBFailedEvent,
    PushNotificationFailedEvent,
    AddMessageDBFailedEvent,
    PushNotificationUpdateFailedEvent,
    SubmitAttachMessageFailedEvent,
    GetMovementsDBFailedEvent,
    GetMovementDBFailedEvent,
    GetMovementMessagesDBFailedEvent,
    GetMovementMessageDBFailedEvent,
    PushPullNotificationGetBoxFailedEvent,
    CustomerRequestedMissingMovement,
    NCTSRequestedMissingMovement,
    TraderToNCTSSubmissionSuccessful,
    NCTSToTraderSubmissionSuccessful,
    LargeMessageSubmissionRequested
  )

  case object AmendmentAcceptance            extends AuditType("AmendmentAcceptance", transitMovementsRouter, Some(MessageType.IE004), None)
  case object ArrivalNotification            extends AuditType("ArrivalNotification", commonTransitConventionTraders, Some(MessageType.IE007), None)
  case object InvalidationDecision           extends AuditType("InvalidationDecision", transitMovementsRouter, Some(MessageType.IE009), None)
  case object DeclarationAmendment           extends AuditType("DeclarationAmendment", commonTransitConventionTraders, Some(MessageType.IE013), None)
  case object DeclarationInvalidationRequest extends AuditType("DeclarationInvalidationRequest", commonTransitConventionTraders, Some(MessageType.IE014), None)
  case object DeclarationData                extends AuditType("DeclarationData", commonTransitConventionTraders, Some(MessageType.IE015), None)
  case object Discrepancies                  extends AuditType("Discrepancies", transitMovementsRouter, Some(MessageType.IE019), None)
  case object NotificationToAmendDeclaration extends AuditType("NotificationToAmendDeclaration", transitMovementsRouter, Some(MessageType.IE022), None)
  case object GoodsReleasedNotification      extends AuditType("GoodsReleasedNotification", transitMovementsRouter, Some(MessageType.IE025), None)
  case object MRNAllocated                   extends AuditType("MRNAllocated", transitMovementsRouter, Some(MessageType.IE028), None)
  case object ReleaseForTransit              extends AuditType("ReleaseForTransit", transitMovementsRouter, Some(MessageType.IE029), None)
  case object RecoveryNotification           extends AuditType("RecoveryNotification", transitMovementsRouter, Some(MessageType.IE035), None)
  case object UnloadingPermission            extends AuditType("UnloadingPermission", transitMovementsRouter, Some(MessageType.IE043), None)
  case object UnloadingRemarks               extends AuditType("UnloadingRemarks", commonTransitConventionTraders, Some(MessageType.IE044), None)

  case object InformationAboutNonArrivedMovement
      extends AuditType("InformationAboutNonArrivedMovement", commonTransitConventionTraders, Some(MessageType.IE141), None)
  case object WriteOffNotification             extends AuditType("WriteOffNotification", transitMovementsRouter, Some(MessageType.IE045), None)
  case object NoReleaseForTransit              extends AuditType("NoReleaseForTransit", transitMovementsRouter, Some(MessageType.IE051), None)
  case object RequestOfRelease                 extends AuditType("RequestOfRelease", commonTransitConventionTraders, Some(MessageType.IE054), None)
  case object GuaranteeNotValid                extends AuditType("GuaranteeNotValid", transitMovementsRouter, Some(MessageType.IE055), None)
  case object RejectionFromOfficeOfDeparture   extends AuditType("RejectionFromOfficeOfDeparture", transitMovementsRouter, Some(MessageType.IE056), None)
  case object RejectionFromOfficeOfDestination extends AuditType("RejectionFromOfficeOfDestination", transitMovementsRouter, Some(MessageType.IE057), None)
  case object ControlDecisionNotification      extends AuditType("ControlDecisionNotification", transitMovementsRouter, Some(MessageType.IE060), None)

  case object ForwardedIncidentNotificationToED
      extends AuditType("ForwardedIncidentNotificationToED", commonTransitConventionTraders, Some(MessageType.IE182), None)
  case object PositiveAcknowledge         extends AuditType("PositiveAcknowledge", transitMovementsRouter, Some(MessageType.IE928), None)
  case object RequestOnNonArrivedMovement extends AuditType("RequestOnNonArrivedMovement", transitMovementsRouter, Some(MessageType.IE140), None)

  case object LargeMessageSubmissionRequested
      extends AuditType("LargeMessageSubmissionRequested", commonTransitConventionTraders, None, Some(CTCTradersWorkflow))

  case object TraderFailedUploadEvent extends AuditType("TraderFailedUploadEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object SubmitArrivalNotificationFailedEvent
      extends AuditType("SubmitArrivalNotificationFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object SubmitDeclarationFailedEvent extends AuditType("SubmitDeclarationFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))
  case object ValidationFailedEvent        extends AuditType("ValidationFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object CreateMovementDBFailedEvent extends AuditType("CreateMovementDBFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object PushNotificationFailedEvent extends AuditType("PushNotificationFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object AddMessageDBFailedEvent extends AuditType("AddMessageDBFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object PushNotificationUpdateFailedEvent
      extends AuditType("PushNotificationUpdateFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object SubmitAttachMessageFailedEvent extends AuditType("SubmitAttachMessageFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object GetMovementsDBFailedEvent extends AuditType("GetMovementsDBFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object GetMovementDBFailedEvent extends AuditType("GetMovementDBFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object GetMovementMessagesDBFailedEvent
      extends AuditType("GetMovementMessagesDBFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object GetMovementMessageDBFailedEvent extends AuditType("GetMovementMessageDBFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object PushPullNotificationGetBoxFailedEvent
      extends AuditType("PushPullNotificationGetBoxFailedEvent", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object CustomerRequestedMissingMovement
      extends AuditType("CustomerRequestedMissingMovement", commonTransitConventionTraders, None, Some(CTCTradersFailed))

  case object NCTSRequestedMissingMovement extends AuditType("NCTSRequestedMissingMovement", transitMovementsRouter, None, Some(CTCTradersFailed))

  case object PresentationNotificationForThePreLodgedDeclaration
      extends AuditType("PresentationNotificationForThePreLodgedDeclaration", commonTransitConventionTraders, Some(MessageType.IE170), None)

  case object TraderToNCTSSubmissionSuccessful
      extends AuditType("TraderToNCTSSubmissionSuccessful", commonTransitConventionTraders, None, Some(CTCTradersSucceeded))

  case object NCTSToTraderSubmissionSuccessful extends AuditType("NCTSToTraderSubmissionSuccessful", transitMovementsRouter, None, Some(CTCTradersSucceeded))

  def fromName(name: String): Option[AuditType] = values.find(_.name == name)

}
