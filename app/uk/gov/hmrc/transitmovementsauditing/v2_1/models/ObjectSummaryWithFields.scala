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

package uk.gov.hmrc.transitmovementsauditing.v2_1.models

import play.api.libs.json.Json
import play.api.libs.json.Writes
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5

case class ObjectSummaryWithFields(objectSummary: ObjectSummaryWithMd5, fields: Seq[(String, String)])

object ObjectSummaryWithFields {

  implicit val objectSummaryWithFieldsWrites: Writes[ObjectSummaryWithFields] = (o: ObjectSummaryWithFields) =>
    Json.obj(
      "objectSummary"    -> o.objectSummary.toString,
      "additionalFields" -> o.fields.toString()
    )

}
