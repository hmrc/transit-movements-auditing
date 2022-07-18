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
    GoodsReleasedNotification,
    MRNAllocated,
    ReleaseForTransit,
    UnloadingPermission,
    UnloadingRemarks,
    WriteOffNotification,
    NoReleaseForTransit,
    RequestOfRelease,
    GuaranteeNotValid,
    RejectionFromOfficeOfDeparture,
    RejectionFromOfficeOfDestination,
    ControlDecisionNotification,
    PresentationNotificationForThePreLodgedDeclaration,
    PositiveAcknowledge
  )

  case object AmendmentAcceptance              extends AuditType("AmendmentAcceptance", transitMovementsRouter, MessageType.CC015C)
  case object ArrivalNotification              extends AuditType("ArrivalNotification", commonTransitConventionTraders, MessageType.Unknown)
  case object InvalidationDecision             extends AuditType("InvalidationDecision", transitMovementsRouter, MessageType.Unknown)
  case object DeclarationAmendment             extends AuditType("DeclarationAmendment", commonTransitConventionTraders, MessageType.Unknown)
  case object DeclarationInvalidationRequest   extends AuditType("DeclarationInvalidationRequest", commonTransitConventionTraders, MessageType.Unknown)
  case object DeclarationData                  extends AuditType("DeclarationData", commonTransitConventionTraders, MessageType.Unknown)
  case object Discrepancies                    extends AuditType("Discrepancies", transitMovementsRouter, MessageType.Unknown)
  case object GoodsReleasedNotification        extends AuditType("GoodsReleasedNotification", transitMovementsRouter, MessageType.Unknown)
  case object MRNAllocated                     extends AuditType("MRNAllocated", transitMovementsRouter, MessageType.Unknown)
  case object ReleaseForTransit                extends AuditType("ReleaseForTransit", transitMovementsRouter, MessageType.Unknown)
  case object UnloadingPermission              extends AuditType("UnloadingPermission", transitMovementsRouter, MessageType.Unknown)
  case object UnloadingRemarks                 extends AuditType("UnloadingRemarks", commonTransitConventionTraders, MessageType.Unknown)
  case object WriteOffNotification             extends AuditType("WriteOffNotification", transitMovementsRouter, MessageType.Unknown)
  case object NoReleaseForTransit              extends AuditType("NoReleaseForTransit", transitMovementsRouter, MessageType.Unknown)
  case object RequestOfRelease                 extends AuditType("RequestOfRelease", commonTransitConventionTraders, MessageType.Unknown)
  case object GuaranteeNotValid                extends AuditType("GuaranteeNotValid", transitMovementsRouter, MessageType.Unknown)
  case object RejectionFromOfficeOfDeparture   extends AuditType("RejectionFromOfficeOfDeparture", transitMovementsRouter, MessageType.Unknown)
  case object RejectionFromOfficeOfDestination extends AuditType("RejectionFromOfficeOfDestination", transitMovementsRouter, MessageType.Unknown)
  case object ControlDecisionNotification      extends AuditType("ControlDecisionNotification", transitMovementsRouter, MessageType.Unknown)
  case object PositiveAcknowledge              extends AuditType("PositiveAcknowledge", transitMovementsRouter, MessageType.Unknown)

  case object PresentationNotificationForThePreLodgedDeclaration
      extends AuditType("PresentationNotificationForThePreLodgedDeclaration", commonTransitConventionTraders, MessageType.Unknown)

  def fromName(name: String): Option[AuditType] = values.find(_.name == name)

}
