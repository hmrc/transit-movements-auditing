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

package uk.gov.hmrc.transitmovementsauditing.base

import org.apache.pekko.Done
import org.apache.pekko.stream.FlowShape
import org.apache.pekko.stream.scaladsl.Broadcast
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.GraphDSL
import org.apache.pekko.stream.scaladsl.Sink

import scala.concurrent.Future

object TestStreamComponents {

  def flowProbe[A]: Flow[A, A, Future[Done]] =
    Flow.fromGraph(
      GraphDSL.createGraph(Sink.ignore) {
        implicit builder => s1 =>
          import GraphDSL.Implicits.*

          val broadcast = builder.add(Broadcast[A](2))
          broadcast.out(0) ~> s1.in
          FlowShape(broadcast.in, broadcast.out(1))
      }
    )

}
