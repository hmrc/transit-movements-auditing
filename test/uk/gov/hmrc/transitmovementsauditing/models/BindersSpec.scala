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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import Binders.auditTypePathBindable
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.AmendmentAcceptance

class BindersSpec extends AnyFreeSpec with Matchers {

  "AuditType path bindable" - {
    "should bind a valid AuditType" in {
      val result = auditTypePathBindable.bind("auditType", "AmendmentAcceptance")
      result shouldBe Right(AmendmentAcceptance)
    }
    "should fail with an invalid AuditType" in {
      val result = auditTypePathBindable.bind("auditType", "Nonsense")
      result shouldBe Left("Error locating audit type")
    }
    "should unbind a valid AuditType" in {
      val result = auditTypePathBindable.unbind("auditType", AmendmentAcceptance)
      result shouldBe "AmendmentAcceptance"
    }
  }

}
