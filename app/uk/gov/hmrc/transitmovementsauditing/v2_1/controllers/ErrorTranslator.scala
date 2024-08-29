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

package uk.gov.hmrc.transitmovementsauditing.v2_1.controllers

import cats.data.EitherT
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.AuditError
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ConversionError
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ObjectStoreError
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ParseError
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.PresentationError
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.AuditError.Disabled
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ObjectStoreError.FileNotFound
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ObjectStoreError.UnexpectedError
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ParseError.BadDateTime
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ParseError.NoElementFound
import uk.gov.hmrc.transitmovementsauditing.v2_1.models.errors.ParseError.TooManyElementsFound

import java.time.format.DateTimeParseException
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

  implicit val auditError: Converter[AuditError] = new Converter[AuditError] {

    def convert(auditError: AuditError): PresentationError = auditError match {
      case AuditError.UnexpectedError(_, thr) => PresentationError.internalServiceError(cause = thr)
      case AuditError.FailedToParse(thr)      => PresentationError.internalServiceError(cause = Some(thr))
      case Disabled                           => PresentationError.internalServiceError()
    }
  }

  implicit val conversionError: Converter[ConversionError] = new Converter[ConversionError] {

    def convert(conversionError: ConversionError): PresentationError = conversionError match {
      case ConversionError.FailedConversion(_)     => PresentationError.internalServiceError()
      case ConversionError.UnexpectedError(_, thr) => PresentationError.internalServiceError(cause = thr)
    }
  }

  implicit val objectStoreError: Converter[ObjectStoreError] = new Converter[ObjectStoreError] {

    def convert(objectStoreError: ObjectStoreError): PresentationError = objectStoreError match {
      case FileNotFound(fileLocation) => PresentationError.badRequestError(s"file not found at location: $fileLocation")
      case UnexpectedError(ex)        => PresentationError.internalServiceError(cause = ex)
    }
  }

  implicit val parseError: Converter[ParseError] = new Converter[ParseError] {

    def convert(parseError: ParseError): PresentationError = parseError match {
      case NoElementFound(element)                          => PresentationError.badRequestError(s"Element $element not found")
      case TooManyElementsFound(element)                    => PresentationError.badRequestError(s"Found too many elements of type $element")
      case BadDateTime(element, ex: DateTimeParseException) => PresentationError.badRequestError(s"Could not parse datetime for $element: ${ex.getMessage}")
      case ParseError.UnexpectedError(ex)                   => PresentationError.internalServiceError(cause = ex)
      case ParseError.IgnoreElement                         => PresentationError.badRequestError("Element set to Ignore")

    }
  }

}
