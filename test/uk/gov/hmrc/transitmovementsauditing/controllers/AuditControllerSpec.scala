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
import uk.gov.hmrc.transitmovementsauditing.config.Constants.XAuditSourceHeader
import uk.gov.hmrc.transitmovementsauditing.controllers.actions.InternalAuthActionProvider
import uk.gov.hmrc.transitmovementsauditing.generators.ModelGenerators
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.AmendmentAcceptance
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationAmendment
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.DeclarationData
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.SubmitArrivalNotificationFailedEvent
import uk.gov.hmrc.transitmovementsauditing.models.AuditType.TraderFailedUploadEvent
import uk.gov.hmrc.transitmovementsauditing.models.MessageType.IE015
import uk.gov.hmrc.transitmovementsauditing.models.MovementType.Departure
import uk.gov.hmrc.transitmovementsauditing.models.Details
import uk.gov.hmrc.transitmovementsauditing.models.EORINumber
import uk.gov.hmrc.transitmovementsauditing.models.FileId
import uk.gov.hmrc.transitmovementsauditing.models.MessageId
import uk.gov.hmrc.transitmovementsauditing.models.MessageType
import uk.gov.hmrc.transitmovementsauditing.models.Metadata
import uk.gov.hmrc.transitmovementsauditing.models.MovementId
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

  private val xmlStream          = Source.single(ByteString(<test>123</test>.mkString))
  private val jsonStreamMultiple = Source.single(ByteString("""{ "test1": "1235", "test2": "234" } """))
  private val jsonDetailsStream  = Source.single(ByteString("""{ "metadata": {"path": "some-path"}, "payload": { "test": "123" }} """.mkString))

  private val jsonFullDetailsStream = Source.single(
    ByteString(
      """{ "metadata": {"path": "some-path", "movementId": "movementId", "messageId": "messageId", "enrolmentEORI": "enrolmentEORI", "movementType": "departure", "messageType": "IE015"}, "payload": { "test": "123" }} """.mkString
    )
  )
  private val invalidJsonDetailsStream = Source.single(ByteString("""{ "data": {"path": "some-path"}, "payload": { "test": "123" }} """.mkString))

  private val someGoodCC015CJson = Json.obj("test" -> "123")

  private val metadata: Metadata =
    Metadata(
      "some-path",
      Some(MovementId("movementId")),
      Some(MessageId("messageId")),
      Some(EORINumber("enrolmentEORI")),
      Some(Departure),
      Some(IE015)
    )
  private val someValidFullDetails = Details(Some("TraderFailedUploadEvent"), metadata, Some(someGoodCC015CJson))

  private val someValidDetails =
    Details(Some("TraderFailedUploadEvent"), Metadata("some-path", None, None, None, None, None), Some(someGoodCC015CJson))

  private val emptyFakeRequest = FakeRequest("POST", "/")

  private val fakeRequest = emptyFakeRequest
    .withHeaders(
      CONTENT_TYPE                   -> "application/xml",
      Constants.XContentLengthHeader -> contentLessThanAuditLimit,
      Constants.XAuditMetaPath       -> "/customs/transits/movements"
    )
    .withBody(xmlStream)

  private val fakeStatusRequest = emptyFakeRequest
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withBody(jsonDetailsStream)

  private val fakeJsonRequestHeadersWithoutPath = emptyFakeRequest
    .withHeaders(
      CONTENT_TYPE                     -> "application/json",
      Constants.XAuditMetaMovementId   -> "movement-id-1",
      Constants.XAuditMetaMessageId    -> "message-id-1",
      Constants.XAuditMetaEORI         -> "eori-1",
      Constants.XAuditMetaMovementType -> "departure",
      Constants.XAuditMetaMessageType  -> "IE015",
      Constants.XContentLengthHeader   -> contentLessThanAuditLimit
    )
    .withBody(jsonStreamMultiple)

  private val fakeJsonRequestWithAllHeaders = fakeJsonRequestHeadersWithoutPath
    .withHeaders(
      Constants.XAuditMetaPath -> "/customs/transits/movements"
    )

  private val fakeJsonRequestWithAllHeadersAndExceeds = fakeJsonRequestWithAllHeaders
    .withHeaders(
      Constants.XContentLengthHeader -> contentExceedsAuditLimit
    )

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

    "Message type audits" - {

      "returns 202 for a message type audit message when auditing is disabled" in {

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
        when(mockAuditService.sendMessageTypeEvent(eqTo(AmendmentAcceptance), any())(any())).thenReturn(EitherT.rightT(()))

        val result = controller.post(AmendmentAcceptance)(fakeRequest)
        status(result) mustBe Status.ACCEPTED

        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockConversionService, times(1)).toJson(eqTo(MessageType.IE004), any())(any())
        verify(mockAuditService, times(1)).sendMessageTypeEvent(eqTo(AmendmentAcceptance), any())(any())
      }

      "returns 202 when auditing was successful with a payload that does not exceed audit limit" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(eqTo(MessageType.IE013), eqTo(xmlStream))(any())).thenAnswer(conversionServiceXmlToJsonPartial)
        when(mockAuditService.sendMessageTypeEvent(eqTo(DeclarationAmendment), any())(any())).thenReturn(EitherT.rightT(()))

        val result = controller.post(DeclarationAmendment)(fakeJsonRequestWithAllHeaders)
        status(result) mustBe Status.ACCEPTED

        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockConversionService, times(0)).toJson(eqTo(MessageType.IE013), eqTo(xmlStream))(any())
        verify(mockAuditService, times(1)).sendMessageTypeEvent(eqTo(DeclarationAmendment), any())(any())
      }

      "returns 202 when auditing was successful with a payload that exceeds the audit limit" in {

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
        when(mockAuditService.sendMessageTypeEvent(eqTo(DeclarationData), any())(any())).thenReturn(EitherT.rightT(()))

        val objectSummary = arbitraryObjectSummaryWithMd5.arbitrary.sample.get
        when(mockObjectStoreService.putFile(FileId(any()), any())(any[ExecutionContext], any[HeaderCarrier]))
          .thenReturn(EitherT.rightT(objectSummary))

        val result = controller.post(DeclarationData)(
          fakeRequest
            .withHeaders(CONTENT_TYPE -> "application/json", Constants.XContentLengthHeader -> contentExceedsAuditLimit)
        )
        status(result) mustBe Status.ACCEPTED

        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockConversionService, times(0)).toJson(any(), any())(any())
        verify(mockAuditService, times(1)).sendMessageTypeEvent(eqTo(DeclarationData), any())(any())
        verify(mockFieldParsingService, times(1)).getAdditionalFields(any(), any())
        verify(mockObjectStoreService, times(1)).putFile(FileId(any()), any())(any(), any())
      }

      "returns 500 when the conversion service fails" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(any(), any())(any())).thenReturn(EitherT.leftT(ConversionError.UnexpectedError("test error")))

        val result = controller.post(AmendmentAcceptance)(fakeRequest.withHeaders(Constants.XContentLengthHeader -> contentLessThanAuditLimit))
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "code"    -> "INTERNAL_SERVER_ERROR",
          "message" -> "Internal server error"
        )
        verify(mockAuditService, times(0)).sendMessageTypeEvent(any(), any())(any())
      }

      "returns 500 when the audit service fails" in {

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockConversionService.toJson(any(), any())(any())).thenAnswer(conversionServiceXmlToJsonPartial)
        when(mockAuditService.sendMessageTypeEvent(eqTo(AmendmentAcceptance), any())(any())).thenReturn(EitherT.leftT(AuditError.UnexpectedError("test error")))

        val result = controller.post(AmendmentAcceptance)(fakeRequest)
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "code"    -> "INTERNAL_SERVER_ERROR",
          "message" -> "Internal server error"
        )

        verify(mockAuditService, times(1)).sendMessageTypeEvent(eqTo(AmendmentAcceptance), any())(any())
      }

    }

    "Status type audits" - {

      "returns 202 for a message type audit message when auditing is disabled" in {

        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(false)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        // If the config fails, we have something that can't be deserialised, so this will below up.
        val result = controller.post(TraderFailedUploadEvent)(fakeStatusRequest.withBody(Source.single(ByteString())))
        status(result) mustBe Status.ACCEPTED
      }

      "returns 202 when auditing was successful for trader failed upload event" in {

        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(mockAuditService.sendStatusTypeEvent(eqTo(someValidDetails), eqTo("CTCTradersFailed"), eqTo("common-transit-convention-traders"))(any()))
          .thenReturn(EitherT.rightT(()))

        val result = controller.post(TraderFailedUploadEvent)(fakeStatusRequest.withBody(jsonDetailsStream))
        status(result) mustBe Status.ACCEPTED

        verify(mockAuditService, times(1)).sendStatusTypeEvent(
          eqTo(someValidDetails),
          eqTo("CTCTradersFailed"),
          eqTo("common-transit-convention-traders")
        )(any())
      }

      "returns 500 when the audit service fails for trader failed upload event" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(mockAuditService.sendStatusTypeEvent(eqTo(someValidDetails), eqTo("CTCTradersFailed"), eqTo("common-transit-convention-traders"))(any()))
          .thenReturn(EitherT.leftT(AuditError.UnexpectedError("test error")))

        val result = controller.post(TraderFailedUploadEvent)(fakeStatusRequest.withBody(jsonDetailsStream))
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "code"    -> "INTERNAL_SERVER_ERROR",
          "message" -> "Internal server error"
        )

        verify(mockAuditService, times(1)).sendStatusTypeEvent(
          eqTo(someValidDetails),
          eqTo("CTCTradersFailed"),
          eqTo("common-transit-convention-traders")
        )(any())
      }

      "returns 202 when auditing for Status was successful with an valid Details json payload" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(
          mockAuditService.sendStatusTypeEvent(eqTo(someValidDetails), eqTo("CTCTradersFailed"), eqTo("common-transit-convention-traders"))(
            any()
          )
        ).thenReturn(EitherT.rightT(()))

        val result = controller.post(TraderFailedUploadEvent)(fakeStatusRequest.withBody(jsonDetailsStream))
        status(result) mustBe Status.ACCEPTED

        verify(mockAuditService, times(1)).sendStatusTypeEvent(
          eqTo(someValidDetails),
          eqTo("CTCTradersFailed"),
          eqTo("common-transit-convention-traders")
        )(any())
      }

      "returns 202 when auditing for Status was successful with an valid Details json payload along with optional values" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(
          mockAuditService.sendStatusTypeEvent(
            eqTo(someValidFullDetails),
            eqTo("CTCTradersFailed"),
            eqTo("common-transit-convention-traders")
          )(any())
        ).thenReturn(EitherT.rightT(()))

        val result = controller.post(TraderFailedUploadEvent)(fakeStatusRequest.withBody(jsonFullDetailsStream))
        status(result) mustBe Status.ACCEPTED

        verify(mockAuditService, times(1)).sendStatusTypeEvent(
          eqTo(someValidFullDetails),
          eqTo("CTCTradersFailed"),
          eqTo("common-transit-convention-traders")
        )(any())
      }

      "returns 202 when auditing for Status was successful when audit source header is passed" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(
          mockAuditService.sendStatusTypeEvent(eqTo(someValidDetails), eqTo("CTCTradersFailed"), eqTo("test"))(any())
        ).thenReturn(EitherT.rightT(()))

        val request = emptyFakeRequest.withHeaders(CONTENT_TYPE -> "application/json", XAuditSourceHeader -> "test").withBody(jsonDetailsStream)
        val result  = controller.post(TraderFailedUploadEvent)(request)
        status(result) mustBe Status.ACCEPTED

        verify(mockAuditService, times(1)).sendStatusTypeEvent(
          eqTo(someValidDetails),
          eqTo("CTCTradersFailed"),
          eqTo("test")
        )(any())
      }

      "returns 400 when auditing for Status with an invalid Details json payload" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        val result = controller.post(SubmitArrivalNotificationFailedEvent)(fakeStatusRequest.withBody(invalidJsonDetailsStream))
        status(result) mustBe Status.BAD_REQUEST
      }

      "returns 500 when auditing for Status with an empty payload" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        val result = controller.post(SubmitArrivalNotificationFailedEvent)(fakeStatusRequest.withBody())
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
      }

      "returns 400 when path header is not present" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        val result = controller.post(DeclarationData)(fakeJsonRequestHeadersWithoutPath)
        status(result) mustBe Status.BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "code"    -> "BAD_REQUEST",
          "message" -> "X-Audit-Meta-Path is missing"
        )

        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockAuditService, times(0)).sendMessageTypeEvent(any(), any())(any())
      }

      "returns 500 when path header is present and Unexpected error occurs" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockAuditService.sendMessageTypeEvent(any(), any())(any()))
          .thenReturn(EitherT.leftT(AuditError.UnexpectedError("test error")))

        val result = controller.post(DeclarationData)(fakeJsonRequestWithAllHeaders)
        status(result) mustBe Status.INTERNAL_SERVER_ERROR

        contentAsJson(result) mustBe Json.obj(
          "code"    -> "INTERNAL_SERVER_ERROR",
          "message" -> "Internal server error"
        )
        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockAuditService, times(1)).sendMessageTypeEvent(any(), any())(any())
      }

      "returns 202 when path header is present" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)
        when(mockAuditService.sendMessageTypeEvent(eqTo(DeclarationData), any())(any()))
          .thenReturn(EitherT.rightT(()))

        val result = controller.post(DeclarationData)(fakeJsonRequestWithAllHeaders)
        status(result) mustBe Status.ACCEPTED

        verify(mockAppConfig, times(1)).auditMessageMaxSize
        verify(mockAuditService, times(1)).sendMessageTypeEvent(eqTo(DeclarationData), any())(any())
      }

      "returns 202 when path header is present and message is large" in {
        reset(mockAppConfig)
        when(mockAppConfig.auditingEnabled).thenReturn(true)
        when(mockAppConfig.auditMessageMaxSize).thenReturn(50000)

        when(mockAuditService.sendMessageTypeEvent(eqTo(DeclarationData), any())(any()))
          .thenReturn(EitherT.rightT(()))

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

        val objectSummary = arbitraryObjectSummaryWithMd5.arbitrary.sample.get
        when(mockObjectStoreService.putFile(FileId(any()), any())(any[ExecutionContext], any[HeaderCarrier]))
          .thenReturn(EitherT.rightT(objectSummary))

        val result = controller.post(DeclarationData)(fakeJsonRequestWithAllHeadersAndExceeds)
        status(result) mustBe Status.ACCEPTED

        verify(mockAuditService, times(1)).sendMessageTypeEvent(eqTo(DeclarationData), any())(any())
      }

    }
  }
}
