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

package uk.gov.hmrc.transitmovementsauditing.models

import Sources._

sealed abstract class AuditType(val name: String, val source: String, val messageType: MessageType) extends Product with Serializable

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
    WriteOffNotification,
    NoReleaseForTransit,
    RequestOfRelease,
    GuaranteeNotValid,
    RejectionFromOfficeOfDeparture,
    RejectionFromOfficeOfDestination,
    PresentationNotificationForThePreLodgedDeclaration,
    ControlDecisionNotification,
    ForwardedIncidentNotificationToED,
    PositiveAcknowledge
  )

  case object AmendmentAcceptance               extends AuditType("AmendmentAcceptance", transitMovementsRouter, MessageType.CC004C)
  case object ArrivalNotification               extends AuditType("ArrivalNotification", commonTransitConventionTraders, MessageType.CC007C)
  case object InvalidationDecision              extends AuditType("InvalidationDecision", transitMovementsRouter, MessageType.CC009C)
  case object DeclarationAmendment              extends AuditType("DeclarationAmendment", commonTransitConventionTraders, MessageType.CC013C)
  case object DeclarationInvalidationRequest    extends AuditType("DeclarationInvalidationRequest", commonTransitConventionTraders, MessageType.CC014C)
  case object DeclarationData                   extends AuditType("DeclarationData", commonTransitConventionTraders, MessageType.CC015C)
  case object Discrepancies                     extends AuditType("Discrepancies", transitMovementsRouter, MessageType.CC019C)
  case object NotificationToAmendDeclaration    extends AuditType("NotificationToAmendDeclaration", transitMovementsRouter, MessageType.CC022C)
  case object GoodsReleasedNotification         extends AuditType("GoodsReleasedNotification", transitMovementsRouter, MessageType.CC025C)
  case object MRNAllocated                      extends AuditType("MRNAllocated", transitMovementsRouter, MessageType.CC028C)
  case object ReleaseForTransit                 extends AuditType("ReleaseForTransit", transitMovementsRouter, MessageType.CC029C)
  case object RecoveryNotification              extends AuditType("RecoveryNotification", transitMovementsRouter, MessageType.CC035C)
  case object UnloadingPermission               extends AuditType("UnloadingPermission", transitMovementsRouter, MessageType.CC043C)
  case object UnloadingRemarks                  extends AuditType("UnloadingRemarks", commonTransitConventionTraders, MessageType.CC044C)
  case object WriteOffNotification              extends AuditType("WriteOffNotification", transitMovementsRouter, MessageType.CC045C)
  case object NoReleaseForTransit               extends AuditType("NoReleaseForTransit", transitMovementsRouter, MessageType.CC051C)
  case object RequestOfRelease                  extends AuditType("RequestOfRelease", commonTransitConventionTraders, MessageType.CC054C)
  case object GuaranteeNotValid                 extends AuditType("GuaranteeNotValid", transitMovementsRouter, MessageType.CC055C)
  case object RejectionFromOfficeOfDeparture    extends AuditType("RejectionFromOfficeOfDeparture", transitMovementsRouter, MessageType.CC056C)
  case object RejectionFromOfficeOfDestination  extends AuditType("RejectionFromOfficeOfDestination", transitMovementsRouter, MessageType.CC057C)
  case object ControlDecisionNotification       extends AuditType("ControlDecisionNotification", transitMovementsRouter, MessageType.CC060C)
  case object ForwardedIncidentNotificationToED extends AuditType("ForwardedIncidentNotificationToED", commonTransitConventionTraders, MessageType.CC182C)
  case object PositiveAcknowledge               extends AuditType("PositiveAcknowledge", transitMovementsRouter, MessageType.CC928C)

  case object PresentationNotificationForThePreLodgedDeclaration
      extends AuditType("PresentationNotificationForThePreLodgedDeclaration", commonTransitConventionTraders, MessageType.CC170C)

  def fromName(name: String): Option[AuditType] = values.find(_.name == name)

}
