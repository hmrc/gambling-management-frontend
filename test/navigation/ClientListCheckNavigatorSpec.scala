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

package navigation

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ClientListCheckNavigatorSpec extends AnyFreeSpec with Matchers {

  private val navigator = new ClientListCheckNavigator()

  "ClientListCheckNavigator builds SecurityCheck calls per return target" in {
    navigator.agentLanding("u1").url          must include("agent-landing")
    navigator.agentLanding("u1").url          must include("u1")
    navigator.clientList.url                  must include("client-list")
    navigator.manageClientDetails.url         must include("manage-client-details")
    navigator.changeClientReference("u2").url must (include("change-client-reference") and include("u2"))
    navigator.removeClient("u3").url          must (include("remove-client") and include("u3"))
    navigator.clientList.method               mustBe "GET"
  }
}
