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

import viewmodels.agent.ClientListViewModel

object AgentClientUtil {
  val clientsList: List[AgentClient] = List(
    AgentClient(
      regime = "MGD",
      regNumber = "XVM00000001001",
      clientName = Some("ABC Ltd"),
      agentOwnRef = Some("EZ00100")
    ),
    AgentClient(
      regime = "MGD",
      regNumber = "XAM00000001002",
      clientName = Some("XYZ Builders"),
      agentOwnRef = Some("CD002")
    ),
    AgentClient(
      regime = "MGD",
      regNumber = "XCM00000001003",
      clientName = Some("Betelgeuse Construction 3"),
      agentOwnRef = Some("UP374")
    ),
    AgentClient(
      regime = "MGD",
      regNumber = "XEM00000001004",
      clientName = Some("Heritage Contractors 4"),
      agentOwnRef = Some("WY080")
    )
  )

  val clientsListAsViewModel: Seq[ClientListViewModel] = List(
    ClientListViewModel(
      regNumber = "XVM00000001001",
      clientName = "ABC Ltd",
      clientReference = "EZ00100"
    ),
    ClientListViewModel(
      regNumber = "XAM00000001002",
      clientName = "XYZ Builders",
      clientReference = "CD002"
    ),
    ClientListViewModel(
      regNumber = "XCM00000001003",
      clientName = "Betelgeuse Construction 3",
      clientReference = "UP374"
    ),
    ClientListViewModel(
      regNumber = "XEM00000001004",
      clientName = "Heritage Contractors 4",
      clientReference = "WY080"
    )
  )
}
