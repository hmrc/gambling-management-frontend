/*
 * Copyright 2026 HM Revenue & Customs
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

package models.agent

import play.api.libs.json.{Json, OFormat}

/** A single client in an agent's client list. Keyed by (regime, regNumber); `regNumber` becomes the working
  * registration number once the client is selected.
  */
case class AgentClient(
  uniqueId: String,
  regime: String,
  regNumber: String,
  clientName: Option[String],
  agentOwnRef: Option[String]
)

object AgentClient {
  implicit val format: OFormat[AgentClient] = Json.format[AgentClient]
}
