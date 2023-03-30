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

package uk.gov.hmrc.transitmovementsauditing.controllers

import cats.data.EitherT
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError
import uk.gov.hmrc.transitmovementsauditing.models.errors.ConversionError
import uk.gov.hmrc.transitmovementsauditing.models.errors.ObjectStoreError
import uk.gov.hmrc.transitmovementsauditing.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError.Disabled
import uk.gov.hmrc.transitmovementsauditing.models.errors.ObjectStoreError.FileNotFound
import uk.gov.hmrc.transitmovementsauditing.models.errors.ObjectStoreError.UnexpectedError

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait ErrorTranslator {

  implicit class ErrorConverter[E, A](value: EitherT[Future, E, A]) {

    def asPresentation(implicit c: Converter[E], ec: ExecutionContext): EitherT[Future, PresentationError, A] =
      value.leftMap(c.convert)
  }

  trait Converter[E] {
    def convert(input: E): PresentationError
  }

  implicit val auditError = new Converter[AuditError] {

    def convert(auditError: AuditError): PresentationError = auditError match {
      case AuditError.UnexpectedError(_, thr) => PresentationError.internalServiceError(cause = thr)
      case AuditError.FailedToParse(thr)      => PresentationError.internalServiceError(cause = Some(thr))
      case Disabled                           => PresentationError.internalServiceError()
    }
  }

  implicit val conversionError = new Converter[ConversionError] {

    def convert(conversionError: ConversionError): PresentationError = conversionError match {
      case ConversionError.FailedConversion(_)     => PresentationError.internalServiceError()
      case ConversionError.UnexpectedError(_, thr) => PresentationError.internalServiceError(cause = thr)
    }
  }

  implicit val objectStoreError = new Converter[ObjectStoreError] {

    def convert(objectStoreError: ObjectStoreError): PresentationError = objectStoreError match {
      case FileNotFound(fileLocation) => PresentationError.badRequestError(s"file not found at location: $fileLocation")
      case UnexpectedError(ex)        => PresentationError.internalServiceError(cause = ex)
    }
  }

}
