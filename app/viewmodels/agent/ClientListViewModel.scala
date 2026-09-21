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

package viewmodels.agent

import models.agent.AgentClient

case class ClientListViewModel(
  clientName: String,
  regNumber: String,
  clientReference: String
)

object ClientListViewModel {

  def fromAgentClients(clients: List[AgentClient]): Seq[ClientListViewModel] =
    clients.map { client =>
      ClientListViewModel(
        clientName = client.clientName.getOrElse(""),
        regNumber = client.regNumber,
        clientReference = client.agentOwnRef.getOrElse("")
      )
    }

  def filterByName(query: String, clients: Seq[ClientListViewModel]): Seq[ClientListViewModel] = {
    val trimmed = query.trim.toLowerCase
    if (trimmed.isEmpty) clients
    else clients.filter(_.clientName.toLowerCase.contains(trimmed))
  }
}
