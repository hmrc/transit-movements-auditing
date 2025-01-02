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
import com.fasterxml.jackson.core.JsonParseException
import org.mockito.Mockito
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError.Disabled
import uk.gov.hmrc.transitmovementsauditing.models.errors.{AuditError, ConversionError, PresentationError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class ErrorTranslatorSpec extends AnyFreeSpec with Matchers with OptionValues with ScalaFutures with MockitoSugar {

  object Harness extends ErrorTranslator

  import Harness.*

  "ErrorConverter#asPresentation" - {
    "for a success returns the same right" in {
      val input: EitherT[Future, AuditError, Unit] = EitherT.rightT(())
      whenReady(input.asPresentation.value) {
        _ mustBe Right(())
      }
    }

    "for an error returns a left with the appropriate presentation error" in {
      val input: EitherT[Future, AuditError, Unit] = EitherT.leftT(AuditError.Disabled)
      whenReady(input.asPresentation.value) {
        _ mustBe Left(PresentationError.internalServiceError())
      }
    }
  }

  "Audit Error" - {

    "an Unexpected Error with no exception returns an internal service error with no exception" in {
      val input  = AuditError.UnexpectedError("Unexpected", None)
      val output = PresentationError.internalServiceError()

      auditError.convert(input) mustBe output
    }

    "an Unexpected Error with an exception returns an internal service error with an exception" in {
      val exception = new IllegalStateException()
      val input     = AuditError.UnexpectedError("Unexpected Error", Some(exception))
      val output    = PresentationError.internalServiceError(cause = Some(exception))

      auditError.convert(input) mustBe output
    }

    "an FailedToParse returns an internal service error" in {
      val exception = Try(Json.parse("{ no: ")).failed.get.asInstanceOf[JsonParseException]
      val input     = AuditError.FailedToParse(exception)
      val output    = PresentationError.internalServiceError(cause = Some(exception))

      auditError.convert(input) mustBe output
    }

    "a Disabled returns an internal service error" in {
      val output = PresentationError.internalServiceError()

      auditError.convert(Disabled) mustBe output
    }

  }

  "Conversion Error" - {

    "a FailedConversion returns an internal service error with no exception" in {
      val input  = ConversionError.FailedConversion("Unknown Error")
      val output = PresentationError.internalServiceError()

      conversionError.convert(input) mustBe output
    }

    "an Unexpected Error with no exception returns an internal service error with no exception" in {
      val input  = ConversionError.UnexpectedError("Unknown Error")
      val output = PresentationError.internalServiceError()

      conversionError.convert(input) mustBe output
    }

    "an Unexpected Error with an exception returns an internal service error with an exception" in {
      val exception = new IllegalStateException()
      val input     = ConversionError.UnexpectedError("Unknown Error", Some(exception))
      val output    = PresentationError.internalServiceError(cause = Some(exception))

      conversionError.convert(input) mustBe output
    }
  }

}
