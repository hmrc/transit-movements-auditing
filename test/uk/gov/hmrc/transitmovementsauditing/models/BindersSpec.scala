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
