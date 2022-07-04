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

sealed abstract class AuditType(val name: String, val source: String) extends Product with Serializable

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

  case object AmendmentAcceptance              extends AuditType("AmendmentAcceptance", transitMovementsRouter)
  case object ArrivalNotification              extends AuditType("ArrivalNotification", commonTransitConventionTraders)
  case object InvalidationDecision             extends AuditType("InvalidationDecision", transitMovementsRouter)
  case object DeclarationAmendment             extends AuditType("DeclarationAmendment", commonTransitConventionTraders)
  case object DeclarationInvalidationRequest   extends AuditType("DeclarationInvalidationRequest", commonTransitConventionTraders)
  case object DeclarationData                  extends AuditType("DeclarationData", commonTransitConventionTraders)
  case object Discrepancies                    extends AuditType("Discrepancies", transitMovementsRouter)
  case object GoodsReleasedNotification        extends AuditType("GoodsReleasedNotification", transitMovementsRouter)
  case object MRNAllocated                     extends AuditType("MRNAllocated", transitMovementsRouter)
  case object ReleaseForTransit                extends AuditType("ReleaseForTransit", transitMovementsRouter)
  case object UnloadingPermission              extends AuditType("UnloadingPermission", transitMovementsRouter)
  case object UnloadingRemarks                 extends AuditType("UnloadingRemarks", commonTransitConventionTraders)
  case object WriteOffNotification             extends AuditType("WriteOffNotification", transitMovementsRouter)
  case object NoReleaseForTransit              extends AuditType("NoReleaseForTransit", transitMovementsRouter)
  case object RequestOfRelease                 extends AuditType("RequestOfRelease", commonTransitConventionTraders)
  case object GuaranteeNotValid                extends AuditType("GuaranteeNotValid", transitMovementsRouter)
  case object RejectionFromOfficeOfDeparture   extends AuditType("RejectionFromOfficeOfDeparture", transitMovementsRouter)
  case object RejectionFromOfficeOfDestination extends AuditType("RejectionFromOfficeOfDestination", transitMovementsRouter)
  case object ControlDecisionNotification      extends AuditType("ControlDecisionNotification", transitMovementsRouter)
  case object PositiveAcknowledge              extends AuditType("PositiveAcknowledge", transitMovementsRouter)

  case object PresentationNotificationForThePreLodgedDeclaration
      extends AuditType("PresentationNotificationForThePreLodgedDeclaration", commonTransitConventionTraders)

  def fromName(name: String): Option[AuditType] = values.find(_.name == name)

}
