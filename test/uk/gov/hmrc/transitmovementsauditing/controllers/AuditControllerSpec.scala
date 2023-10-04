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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.reset
import org.mockito.MockitoSugar.times
import org.mockito.MockitoSugar.verify
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpErrorConfig
import play.api.http.Status
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.PlayBodyParsers
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.IAAction
import uk.gov.hmrc.internalauth.client.Predicate
import uk.gov.hmrc.internalauth.client.Resource
import uk.gov.hmrc.internalauth.client.ResourceLocation
import uk.gov.hmrc.internalauth.client.ResourceType
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.config.Constants
import uk.gov.hmrc.transitmovementsauditing.controllers.actions.InternalAuthActionProvider
import uk.gov.hmrc.transitmovementsauditing.generators.ModelGenerators
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.AmendmentAcceptance
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationAmendment
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.LargeMessageSubmissionRequested
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.SubmitArrivalNotificationFailedEvent
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.TraderFailedUploadEvent
import uk.gov.hmrc.transitmovementsauditing.models.FileId
import uk.gov.hmrc.transitmovementsauditing.models.MessageType
import uk.gov.hmrc.transitmovementsauditing.models.errors.AuditError
import uk.gov.hmrc.transitmovementsauditing.models.errors.ConversionError
import uk.gov.hmrc.transitmovementsauditing.models.errors.ParseError
import uk.gov.hmrc.transitmovementsauditing.services.AuditService
import uk.gov.hmrc.transitmovementsauditing.services.ConversionService
import uk.gov.hmrc.transitmovementsauditing.services.FieldParsingService
import uk.gov.hmrc.transitmovementsauditing.services.ObjectStoreService
import uk.gov.hmrc.transitmovementsauditing.services.XmlParsingServiceHelpers

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditControllerSpec
    extends AnyFreeSpec
    with XmlParsingServiceHelpers
    with Matchers
    with TestActorSystem
    with MockitoSugar
    with ModelGenerators
    with BeforeAndAfterEach {

  private val contentLessThanAuditLimit = "49999"
  private val contentExceedsAuditLimit  = "50001"

  private val xmlStream         = Source.single(ByteString(<test>123</test>.mkString))
  private val jsonStream        = Source.single(ByteString("""{ "test": "123" } """))
  private val jsonDetailsStream = Source.single(ByteString("""{ "metadata": {"path": "some-path"}, "payload": { "test": "123" }} """.mkString))

  private val jsonFullDetailsStream = Source.single(
    ByteString(
      """{ "metadata": {"path": "some-path", "movementId": "movementId", "messageId": "messageId", "enrolmentEORI": "enrolmentEORI", "movementType": "departure", "messageType": "IE015"}, "payload": { "test": "123" }} """.mkString
    )
  )
  private val invalidJsonDetailsStream = Source.single(ByteString("""{ "data": {"path": "some-path"}, "payload": { "test": "123" }} """.mkString))

  private val emptyFakeRequest = FakeRequest("POST", "/")

  private val fakeRequest = emptyFakeRequest
    .withHeaders(CONTENT_TYPE -> "application/xml", Constants.XContentLengthHeader -> contentLessThanAuditLimit)
    .withBody(xmlStream)

  private val fakeStatusRequest = emptyFakeRequest
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withBody(jsonDetailsStream)

  private val fakeJsonRequest = emptyFakeRequest
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withBody(jsonStream)

  private val mockAppConfig           = mock[AppConfig]
  private val mockAuditService        = mock[AuditService]
  private val mockConversionService   = mock[ConversionService]
  private val mockObjectStoreService  = mock[ObjectStoreService]
  private val mockFieldParsingService = mock[FieldParsingService]

  object TestInternalAuthActionProvider extends InternalAuthActionProvider {

    override def apply(predicate: Predicate)(implicit ec: ExecutionContext): ActionBuilder[Request, AnyContent] =
      predicate match {
        case Predicate.Permission(Resource(ResourceType("transit-movements-auditing"), ResourceLocation("audit")), IAAction("WRITE")) =>
          DefaultActionBuilder(stubControllerComponents().parsers.anyContent)(ec)
        case _ => fail("Predicate is not as expected")
      }
  }

  private val conversionServiceXmlToJsonPartial: PartialFunction[Any, EitherT[Future, ConversionError, Source[ByteString, _]]] = {
    case _ => EitherT.rightT(Source.single(ByteString(Json.stringify(Json.obj("dummy" -> "dummy")))))
  }

  val errorHandler = new DefaultHttpErrorHandler(HttpErrorConfig(showDevErrors = false, None), None, None)

  implicit val temporaryFileCreator: TemporaryFileCreator = SingletonTemporaryFileCreator

  val controllerComponentWithTempFile: ControllerComponents =
    stubControllerComponents(playBodyParsers = PlayBodyParsers(SingletonTemporaryFileCreator, errorHandler)(materializer))

  private val controller = new AuditController(
    controllerComponentWithTempFile,
    mockConversionService,
    mockAuditService,
    mockObjectStoreService,
    mockFieldParsingService,
    TestInternalAuthActionProvider,
    mockAppConfig
  )(
    materializer,
    temporaryFileCreator
  )

  override def beforeEach(): Unit = {
    reset(mockConversionService)
    reset(mockObjectStoreService)
    reset(mockAuditService)
    reset(mockAppConfig)
    when(mockAppConfig.auditingEnabled).thenReturn(true)
  }

  "POST /" - {

    "Payload sourced from request" - {

      "returns 202 when auditing is disabled" in {

        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(false)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        val result = controller.post(AmendmentAcceptance)(fakeRequest)
        status(result) mustBe Status.ACCEPTED
        verify(mockAppConfig, times(0)).auditMessageMaxSize
        verify(mockConversionService, times(0)).toJson(any(), any())(any())
      }

      "returns 202 when auditing was successful with an XML payload that does not exceed audit limit" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(eqTo(MessageType.IE004), any())(any())).thenAnswer(conversionServiceXmlToJsonPartial)
        when(mockAuditService.send(eqTo(AmendmentAcceptance), any())(any())).thenReturn(EitherT.rightT(()))

        val result = controller.post(AmendmentAcceptance)(fakeRequest)
        status(result) mustBe Status.ACCEPTED

        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockConversionService, times(1)).toJson(eqTo(MessageType.IE004), any())(any())
        verify(mockAuditService, times(1)).send(eqTo(AmendmentAcceptance), any())(any())
      }

      "returns 202 when auditing was successful with a payload that does not exceed audit limit" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(eqTo(MessageType.IE013), eqTo(xmlStream))(any())).thenAnswer(conversionServiceXmlToJsonPartial)
        when(mockAuditService.send(eqTo(DeclarationAmendment), any())(any())).thenReturn(EitherT.rightT(()))

        val result = controller.post(DeclarationAmendment)(fakeRequest.withHeaders(CONTENT_TYPE -> "application/json"))
        status(result) mustBe Status.ACCEPTED

        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockConversionService, times(0)).toJson(eqTo(MessageType.IE013), eqTo(xmlStream))(any())
        verify(mockAuditService, times(1)).send(eqTo(DeclarationAmendment), any())(any())
      }

      //TODO uncomment the below test until we complete the implementation of auditing by status type as LargeMessageSubmissionRequested doesn't have messageType
      /*      "returns 202 when auditing was successful with a payload that exceeds the audit limit" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(any(), any())(any())).thenAnswer(conversionServiceXmlToJsonPartial)
        when(mockFieldParsingService.getAdditionalFields(any(), any()))
          .thenReturn(
            EitherT.rightT(
              Seq(
                Right[ParseError, (String, String)]("key1", "value1"),
                Right[ParseError, (String, String)]("key2", "value2"),
                Right[ParseError, (String, String)]("key3", "value3")
              )
            )
          )
        when(mockAuditService.send(eqTo(LargeMessageSubmissionRequested), any())(any())).thenReturn(EitherT.rightT(()))

        val objectSummary = arbitraryObjectSummaryWithMd5.arbitrary.sample.get
        when(mockObjectStoreService.putFile(FileId(any()), any())(any[ExecutionContext], any[HeaderCarrier]))
          .thenReturn(EitherT.rightT(objectSummary))

        val result = controller.post(LargeMessageSubmissionRequested)(
          fakeRequest
            .withHeaders(CONTENT_TYPE -> "application/json", Constants.XContentLengthHeader -> contentExceedsAuditLimit)
        )
        status(result) mustBe Status.ACCEPTED

        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockConversionService, times(0)).toJson(any(), any())(any())
        verify(mockAuditService, times(1)).send(eqTo(LargeMessageSubmissionRequested), any())(any())
        verify(mockFieldParsingService, times(1)).getAdditionalFields(any(), any())
        verify(mockObjectStoreService, times(1)).putFile(FileId(any()), any())(any(), any())
      }*/

      //TODO uncomment the below test until we complete the implementation of auditing by status type as TraderFailedUploadEvent doesn't have messageType

      /*      "returns 202 when auditing was successful for trader failed upload event" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(any(), eqTo(xmlStream))(any())).thenAnswer(conversionServiceXmlToJsonPartial)
        when(mockAuditService.send(eqTo(TraderFailedUploadEvent), any())(any())).thenReturn(EitherT.rightT(()))

        val result = controller.post(TraderFailedUploadEvent)(fakeJsonRequest)
        status(result) mustBe Status.ACCEPTED

        verify(mockConversionService, times(0)).toJson(any(), any())(any())
        verify(mockAuditService, times(1)).send(eqTo(TraderFailedUploadEvent), any())(any())
      }*/

      "returns 500 when the conversion service fails" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(any(), any())(any())).thenReturn(EitherT.leftT(ConversionError.UnexpectedError("test error")))

        val result = controller.post(AmendmentAcceptance)(fakeRequest.withHeaders(Constants.XContentLengthHeader -> contentLessThanAuditLimit))
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "code"    -> "INTERNAL_SERVER_ERROR",
          "message" -> "Internal server error"
        )
        verify(mockAuditService, times(0)).send(any(), any())(any())
      }

      "returns 500 when the audit service fails" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(any(), any())(any())).thenAnswer(conversionServiceXmlToJsonPartial)
        when(mockAuditService.send(eqTo(AmendmentAcceptance), any())(any())).thenReturn(EitherT.leftT(AuditError.UnexpectedError("test error")))

        val result = controller.post(AmendmentAcceptance)(fakeRequest)
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "code"    -> "INTERNAL_SERVER_ERROR",
          "message" -> "Internal server error"
        )

        verify(mockAuditService, times(1)).send(eqTo(AmendmentAcceptance), any())(any())
      }

      //TODO uncomment the below test until we complete the implementation of auditing by status type as TraderFailedUploadEvent doesn't have messageType

      /*      "returns 500 when the audit service fails for trader failed upload event" in {
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(any(), eqTo(xmlStream))(any())).thenAnswer(conversionServiceXmlToJsonPartial)
        when(mockAuditService.send(eqTo(TraderFailedUploadEvent), any())(any()))
          .thenReturn(EitherT.leftT(AuditError.UnexpectedError("test error")))

        val result = controller.post(TraderFailedUploadEvent)(fakeRequest)
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "code"    -> "INTERNAL_SERVER_ERROR",
          "message" -> "Internal server error"
        )

        verify(mockAuditService, times(1)).send(eqTo(TraderFailedUploadEvent), any())(any())
      }*/

      "returns 202 when auditing for Status was successful with an valid Details json payload" in {
        val result = controller.post(SubmitArrivalNotificationFailedEvent)(fakeStatusRequest)
        status(result) mustBe Status.ACCEPTED
      }

      "returns 202 when auditing for Status was successful with an valid Details json payload along with optional values" in {
        val result = controller.post(SubmitArrivalNotificationFailedEvent)(fakeStatusRequest.withBody(jsonFullDetailsStream))
        status(result) mustBe Status.ACCEPTED
      }

      "returns 400 when auditing for Status with an invalid Details json payload" in {
        val result = controller.post(SubmitArrivalNotificationFailedEvent)(fakeStatusRequest.withBody(invalidJsonDetailsStream))
        status(result) mustBe Status.BAD_REQUEST
      }

      "returns 500 when auditing for Status with an empty payload" in {
        val result = controller.post(SubmitArrivalNotificationFailedEvent)(fakeStatusRequest.withBody())
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
      }

    }
  }
}
