/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementsauditing.v2_1.routing

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.times
import org.mockito.MockitoSugar.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HeaderNames
import play.api.http.HttpErrorConfig
import play.api.http.MimeTypes
import play.api.http.Status.ACCEPTED
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.OK
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.PlayBodyParsers
import play.api.mvc.Request
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.internalauth.client.IAAction
import uk.gov.hmrc.internalauth.client.Predicate
import uk.gov.hmrc.internalauth.client.Resource
import uk.gov.hmrc.internalauth.client.ResourceLocation
import uk.gov.hmrc.internalauth.client.ResourceType
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.transitmovementsauditing.routing.VersionedRoutingController
import uk.gov.hmrc.transitmovementsauditing.routing.routes
import uk.gov.hmrc.transitmovementsauditing.controllers.{AuditController => TransitionalAuditController}
import uk.gov.hmrc.transitmovementsauditing.v2_1.controllers.actions.{InternalAuthActionProvider => FinalInternalAuthActionProvider}
import uk.gov.hmrc.transitmovementsauditing.v2_1.controllers.{AuditController => FinalAuditController}
import uk.gov.hmrc.transitmovementsauditing.v2_1.services.{AuditService => FinalAuditService}
import uk.gov.hmrc.transitmovementsauditing.v2_1.services.{ConversionService => FinalConversionService}
import uk.gov.hmrc.transitmovementsauditing.v2_1.services.{FieldParsingService => FinalFieldParsingService}
import uk.gov.hmrc.transitmovementsauditing.v2_1.services.{ObjectStoreService => FinalObjectStoreService}
import uk.gov.hmrc.transitmovementsauditing.services.{AuditService => TransitionalAuditService}
import uk.gov.hmrc.transitmovementsauditing.services.{ConversionService => TransitionalConversionService}
import uk.gov.hmrc.transitmovementsauditing.services.{FieldParsingService => TransitionalFieldParsingService}
import uk.gov.hmrc.transitmovementsauditing.services.{ObjectStoreService => TransitionalObjectStoreService}
import uk.gov.hmrc.transitmovementsauditing.controllers.actions.{InternalAuthActionProvider => TransitionalInternalAuthActionProvider}

import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class VersionedRoutingControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  "post" should {
    "call the transitional controller when APIVersion non 'final' value has been sent" in new Setup {
      val route   = routes.VersionedRoutingController.post("AmendmentAcceptance")
      val request = FakeRequest(route.method, route.url, FakeHeaders(Seq(Constants.APIVersionHeaderKey -> "anything")), xmlStream)
      val result  = controller.post("AmendmentAcceptance")(request)

      status(result) shouldBe ACCEPTED
    }

    "call the transitional controller when no APIVersion header has been sent" in new Setup {
      val route   = routes.VersionedRoutingController.post("AmendmentAcceptance")
      val request = FakeRequest(route.method, route.url, FakeHeaders(Seq.empty), xmlStream)
      val result  = controller.post("AmendmentAcceptance")(request)

      status(result) shouldBe ACCEPTED
    }

    "call the versioned controller when APIVersion header 'final' has been sent" in new Setup {
      val route   = routes.VersionedRoutingController.post("AmendmentAcceptance")
      val request = FakeRequest(route.method, route.url, FakeHeaders(Seq(Constants.APIVersionHeaderKey -> "final")), xmlStream)
      val result  = controller.post("AmendmentAcceptance")(request)

      status(result) shouldBe ACCEPTED
    }

    "return BAD_REQUEST when auditType is not found in non-versioned models" in new Setup {
      val route   = routes.VersionedRoutingController.post("INVALID")
      val request = FakeRequest(route.method, route.url, FakeHeaders(Seq.empty), xmlStream)
      val result  = controller.post("INVALID")(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return BAD_REQUEST when auditType is not found in versioned models" in new Setup {
      val route   = routes.VersionedRoutingController.post("INVALID")
      val request = FakeRequest(route.method, route.url, FakeHeaders(Seq(Constants.APIVersionHeaderKey -> "final")), xmlStream)
      val result  = controller.post("INVALID")(request)

      status(result) shouldBe BAD_REQUEST
    }
  }

  trait Setup {

    implicit val materializer: Materializer = Materializer(TestActorSystem.system)

    val xmlStream = Source.single(ByteString(<test>123</test>.mkString))

    implicit val mockClock: Clock                     = mock[Clock]
    implicit val tempFilCreator: TemporaryFileCreator = mock[TemporaryFileCreator]

    val errorHandler = new DefaultHttpErrorHandler(HttpErrorConfig(showDevErrors = false, None), None, None)

    val controllerComponentWithTempFile: ControllerComponents =
      stubControllerComponents(playBodyParsers = PlayBodyParsers(SingletonTemporaryFileCreator, errorHandler)(materializer))

    private val mockAppConfig           = mock[AppConfig]
    private val mockAuditService        = mock[FinalAuditService]
    private val mockConversionService   = mock[FinalConversionService]
    private val mockObjectStoreService  = mock[FinalObjectStoreService]
    private val mockFieldParsingService = mock[FinalFieldParsingService]

    private val mockTransitionalAuditService        = mock[TransitionalAuditService]
    private val mockTransitionalConversionService   = mock[TransitionalConversionService]
    private val mockTransitionalObjectStoreService  = mock[TransitionalObjectStoreService]
    private val mockTransitionalFieldParsingService = mock[TransitionalFieldParsingService]

    object TestFinalInternalAuthActionProvider extends FinalInternalAuthActionProvider {

      override def apply(predicate: Predicate)(implicit ec: ExecutionContext): ActionBuilder[Request, AnyContent] =
        predicate match {
          case Predicate.Permission(Resource(ResourceType("transit-movements-auditing"), ResourceLocation("audit")), IAAction("WRITE")) =>
            DefaultActionBuilder(stubControllerComponents().parsers.anyContent)(ec)
          case _ => fail("Predicate is not as expected")
        }
    }

    object TestTransitionalInternalAuthActionProvider extends TransitionalInternalAuthActionProvider {

      override def apply(predicate: Predicate)(implicit ec: ExecutionContext): ActionBuilder[Request, AnyContent] =
        predicate match {
          case Predicate.Permission(Resource(ResourceType("transit-movements-auditing"), ResourceLocation("audit")), IAAction("WRITE")) =>
            DefaultActionBuilder(stubControllerComponents().parsers.anyContent)(ec)
          case _ => fail("Predicate is not as expected")
        }
    }

    implicit val temporaryFileCreator: TemporaryFileCreator = SingletonTemporaryFileCreator

    val mockFinalAuditController: FinalAuditController = new FinalAuditController(
      controllerComponentWithTempFile,
      mockConversionService,
      mockAuditService,
      mockObjectStoreService,
      mockFieldParsingService,
      TestFinalInternalAuthActionProvider,
      mockAppConfig
    )(
      materializer,
      temporaryFileCreator
    )

    val mockTransitionalAuditController: TransitionalAuditController = new TransitionalAuditController(
      controllerComponentWithTempFile,
      mockTransitionalConversionService,
      mockTransitionalAuditService,
      mockTransitionalObjectStoreService,
      mockTransitionalFieldParsingService,
      TestTransitionalInternalAuthActionProvider,
      mockAppConfig
    )(
      materializer,
      temporaryFileCreator
    )

    val controller = new VersionedRoutingController(
      stubControllerComponents(),
      mockTransitionalAuditController,
      mockFinalAuditController
    )

  }
}
