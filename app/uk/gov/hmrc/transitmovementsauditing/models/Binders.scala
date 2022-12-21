/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.mvc.PathBindable

object Binders {

  implicit def auditTypePathBindable(implicit binder: PathBindable[String]): PathBindable[AuditType] = new PathBindable[AuditType] {

    def bind(key: String, value: String): Either[String, AuditType] =
      for {
        name <- binder.bind(key, value)
        at <-
          AuditType.fromName(name) match {
            case None            => Left("Error locating audit type")
            case Some(auditType) => Right(auditType)
          }
      } yield at

    def unbind(key: String, auditType: AuditType): String =
      binder.unbind(key, auditType.toString)

  }

}
