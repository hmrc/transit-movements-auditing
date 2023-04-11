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

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.MimeTypes
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.objectstore.client.Md5Hash
import uk.gov.hmrc.objectstore.client.Object
import uk.gov.hmrc.objectstore.client.ObjectMetadata
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.RetentionPeriod
import uk.gov.hmrc.objectstore.client.Path.File
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.transitmovementsauditing.base.TestActorSystem
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig
import uk.gov.hmrc.transitmovementsauditing.generators.ModelGenerators
import uk.gov.hmrc.transitmovementsauditing.models.ObjectStoreResourceLocation
import uk.gov.hmrc.transitmovementsauditing.models.errors.ObjectStoreError
import uk.gov.hmrc.transitmovementsauditing.models.errors.ObjectStoreError.FileNotFound
import uk.gov.hmrc.transitmovementsauditing.models.errors.ObjectStoreError.UnexpectedError
import uk.gov.hmrc.transitmovementsauditing.services.ObjectStoreServiceImpl

import java.time.Instant
import java.util.UUID.randomUUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ObjectStoreServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with ModelGenerators with ScalaCheckDrivenPropertyChecks with TestActorSystem {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit private val mockClient: PlayObjectStoreClient = mock[PlayObjectStoreClient]
  private val appConfig                                  = mock[AppConfig]

  lazy val filePath = ObjectStoreResourceLocation(
    Path
      .Directory(s"transit-movements-auditing/12345678")
      .file(randomUUID.toString)
      .asUri
  )
  private val fileContents = "<xml>content</xml>"

  "ObjectStoreService" - {

    "on retrieving the object" - {

      "should return the file contents" in {
        val metadata = ObjectMetadata("", 0, Md5Hash(""), Instant.now(), Map.empty[String, String])
        val obj      = Option[Object[Source[ByteString, NotUsed]]](Object.apply(File(filePath.value), Source.single(ByteString(fileContents)), metadata))

        when(mockClient.getObject[Source[ByteString, NotUsed]](eqTo(File(filePath.value)), any())(any(), any()))
          .thenReturn(Future.successful(obj))

        val sut    = new ObjectStoreServiceImpl(appConfig)
        val result = sut.getContents(filePath).value

        whenReady(result) {
          _ mustBe Right(obj.get.content)
        }

      }

      "should return an error when the file is not found in object store" in {
        when(mockClient.getObject[Source[ByteString, NotUsed]](any[File](), any())(any(), any())).thenReturn(Future.successful(None))
        val sut    = new ObjectStoreServiceImpl(appConfig)
        val result = sut.getContents(filePath).value

        whenReady(result) {
          case Left(FileNotFound(fileName)) => succeed
          case reason                       => fail(s"Expected Left(FileNotFound), got $reason")
        }
      }

      "should return an error when there is a problem getting an object from object store" in {
        when(mockClient.getObject[Source[ByteString, NotUsed]](any[File](), any())(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("failed", INTERNAL_SERVER_ERROR)))
        val sut    = new ObjectStoreServiceImpl(appConfig)
        val result = sut.getContents(filePath).value

        whenReady(result) {
          case Left(UnexpectedError(fileName)) => succeed
          case reason                          => fail(s"Expected an UnexpectedError, got $reason")
        }
      }
    }

    "On putting a file in object store" - {

      val source: Source[ByteString, _]       = Source.single(ByteString("<test>test</test>"))
      val fileId                              = "xmlFileToStore"
      val objectSummary: ObjectSummaryWithMd5 = arbitraryObjectSummaryWithMd5.arbitrary.sample.get

      "given a successful response, should return a Right with an Object Store Summary" in {

        when(appConfig.appName).thenReturn("transit-movements-auditing")

        when(
          mockClient.putObject(
            eqTo(Path.Directory("auditing").file(fileId)),
            eqTo(source),
            any[RetentionPeriod],
            eqTo(Some(MimeTypes.XML)),
            contentMd5 = any[Option[Md5Hash]],
            eqTo("transit-movements-auditing")
          )(any(), any())
        )
          .thenReturn(Future.successful(objectSummary))

        val sut    = new ObjectStoreServiceImpl(appConfig)(materializer, mockClient)
        val result = sut.putFile(fileId, source)
        whenReady(result.value) {
          _ mustBe Right(objectSummary)
        }

        verify(mockClient, times(1)).putObject(
          eqTo(Path.Directory("auditing").file(fileId)),
          eqTo(source),
          any[RetentionPeriod],
          eqTo(Some(MimeTypes.XML)),
          any(),
          eqTo("transit-movements-auditing")
        )(any(), any())
      }

      "on a failed submission of the content in Object store, it should return an Unexpected error" in {

        val source: Source[ByteString, _] = Source.single(ByteString("<test>test</test>"))

        val error = ObjectStoreError.UnexpectedError(Some(new Throwable("test")))

        when(
          mockClient.putObject(
            eqTo(Path.Directory("auditing").file(fileId)),
            eqTo(source),
            any[RetentionPeriod],
            eqTo(Some(MimeTypes.XML)),
            contentMd5 = any[Option[Md5Hash]],
            eqTo("transit-movements-auditing")
          )(any(), any())
        ).thenReturn(Future.failed(error))

        val sut    = new ObjectStoreServiceImpl(appConfig)(materializer, mockClient)
        val result = sut.putFile(fileId, source)

        whenReady(result.value) {
          case Left(_: ObjectStoreError.UnexpectedError) =>
            verify(mockClient, times(1)).putObject(
              eqTo(Path.Directory("auditing").file(fileId)),
              eqTo(source),
              any[RetentionPeriod],
              eqTo(Some(MimeTypes.XML)),
              contentMd5 = any[Option[Md5Hash]],
              eqTo("transit-movements-auditing")
            )(any(), any())
          case err =>
            fail(s"Expected Left(ObjectStoreError.UnexpectedError), instead got $err")
        }
      }

    }
  }
}
