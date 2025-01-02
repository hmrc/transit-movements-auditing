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

package uk.gov.hmrc.transitmovementsauditing.v2_1.controllers.actions

import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.ActionBuilder
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.transitmovementsauditing.config.AppConfig

import scala.concurrent.ExecutionContext.Implicits.global

class InternalAuthActionProviderSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  type AuthReq[R] = AuthenticatedRequest[R, Unit]

  "InternalAuthActionProvider#apply" - {

    "when internal auth is disabled" - {

      "return action should not be the auth action" in {

        val appConfig = mock[AppConfig]
        when(appConfig.internalAuthEnabled).thenReturn(false)

        val mockBackendAuthComponents = mock[BackendAuthComponents]

        val samplePermission =
          Predicate.Permission(Resource(ResourceType("transit-movements"), ResourceLocation("movement")), IAAction("WRITE"))

        val _ = new InternalAuthActionProviderImpl(appConfig, mockBackendAuthComponents, stubControllerComponents()).apply(samplePermission)

        verify(mockBackendAuthComponents, times(0)).authorizedAction(samplePermission)
      }

    }

    "when internal auth is enabled" - {

      "return action should not be the auth action" in {

        val appConfig = mock[AppConfig]
        when(appConfig.internalAuthEnabled).thenReturn(true)

        val mockBackendAuthComponents = mock[BackendAuthComponents]

        val samplePermission =
          Predicate.Permission(Resource(ResourceType("transit-movements"), ResourceLocation("movement")), IAAction("WRITE"))
        val _ = new InternalAuthActionProviderImpl(appConfig, mockBackendAuthComponents, stubControllerComponents()).apply(samplePermission)

        verify(mockBackendAuthComponents, times(1)).authorizedAction(samplePermission)
      }

    }
  }

}
