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

sealed trait AuditType

object AuditType extends Enumeration {

  val AmendmentAcceptance, ArrivalNotification, InvalidationDecision, DeclarationAmendment, DeclarationInvalidationRequest, DeclarationData, Discrepancies,
    GoodsReleasedNotification, MRNAllocated, ReleaseForTransit, UnloadingPermission, UnloadingRemarks, WriteOffNotification, NoReleaseForTransit,
    RequestOfRelease, GuaranteeNotValid, RejectionFromOfficeOfDeparture, RejectionFromOfficeOfDestination, ControlDecisionNotification,
    PresentationNotificationForThePreLodgedDeclaration, PositiveAcknowledge =
    Value
}
//object AuditType extends AuditType {
//
//  def from(str: String): AuditType =
//    str match {
//      case "AmendmentAcceptance" => AmendmentAcceptance
//      case _                     => Unknown
//    }
//}
//
//case object AmendmentAcceptance                                extends AuditType
//case object ArrivalNotification                                extends AuditType
//case object InvalidationDecision                               extends AuditType
//case object DeclarationAmendment                               extends AuditType
//case object DeclarationInvalidationRequest                     extends AuditType
//case object DeclarationData                                    extends AuditType
//case object Discrepancies                                      extends AuditType
//case object GoodsReleasedNotification                          extends AuditType
//case object MRNAllocated                                       extends AuditType
//case object ReleaseForTransit                                  extends AuditType
//case object UnloadingPermission                                extends AuditType
//case object UnloadingRemarks                                   extends AuditType
//case object WriteOffNotification                               extends AuditType
//case object NoReleaseForTransit                                extends AuditType
//case object RequestOfRelease                                   extends AuditType
//case object GuaranteeNotValid                                  extends AuditType
//case object RejectionFromOfficeOfDeparture                     extends AuditType
//case object RejectionFromOfficeOfDestination                   extends AuditType
//case object ControlDecisionNotification                        extends AuditType
//case object PresentationNotificationForThePreLodgedDeclaration extends AuditType
//case object PositiveAcknowledge                                extends AuditType
