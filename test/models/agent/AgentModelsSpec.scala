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

import models.{GetClientListStatusResponse, requests}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

class AgentModelsSpec extends AnyFreeSpec with Matchers {

  "AgentClient round-trips through JSON" in {
    val model = AgentClient("u1", "mgd", "XMM00000000123", Some("Acme"), Some("ref"))
    Json.toJson(model).as[AgentClient] mustBe model
    Json.parse("""{"uniqueId":"u1","regime":"mgd","regNumber":"RN","clientName":null,"agentOwnRef":null}""")
      .as[AgentClient] mustBe AgentClient("u1", "mgd", "RN", None, None)
  }

  "AgentClientData round-trips through JSON" in {
    val model = AgentClientData("u1", "mgd", "XMM00000000123", Some("Acme"))
    Json.toJson(model).as[AgentClientData] mustBe model
  }

  "UpdateAgentClientRequest round-trips through JSON" in {
    val model = UpdateAgentClientRequest("mgd", "XMM00000000123", "newref")
    Json.toJson(model).as[UpdateAgentClientRequest] mustBe model
  }

  "RemoveAgentClientRequest round-trips through JSON" in {
    val model = requests.RemoveAgentClientRequest("mgd", "XMM00000000123")
    Json.toJson(model).as[requests.RemoveAgentClientRequest] mustBe model
  }

  "HasClientResponse reads from JSON" in {
    Json.parse("""{"hasClient":true}""").as[HasClientResponse] mustBe HasClientResponse(true)
    Json.parse("""{"hasClient":false}""").as[HasClientResponse] mustBe HasClientResponse(false)
  }

  "GetClientListStatusResponse reads a status result" in {
    Json.parse("""{"result":"succeeded"}""").as[GetClientListStatusResponse] mustBe
      GetClientListStatusResponse(ClientListStatus.Succeeded)
  }

  "ClientListStatus" - {
    "reads each known status string" in {
      Json.fromJson[ClientListStatus](JsString("initiate-download")).get mustBe ClientListStatus.InitiateDownload
      Json.fromJson[ClientListStatus](JsString("in-progress")).get mustBe ClientListStatus.InProgress
      Json.fromJson[ClientListStatus](JsString("succeeded")).get mustBe ClientListStatus.Succeeded
      Json.fromJson[ClientListStatus](JsString("failed")).get mustBe ClientListStatus.Failed
    }
    "fails for an unknown status" in {
      Json.fromJson[ClientListStatus](JsString("nope")).isError mustBe true
    }
    "exposes asString for each status" in {
      ClientListStatus.InitiateDownload.asString mustBe "initiate-download"
      ClientListStatus.InProgress.asString mustBe "in-progress"
      ClientListStatus.Succeeded.asString mustBe "succeeded"
      ClientListStatus.Failed.asString mustBe "failed"
    }
  }

  "ClientListCheckReturnTarget exposes a key per target" in {
    ClientListCheckReturnTarget.AgentLanding.key must not be empty
    ClientListCheckReturnTarget.ClientList.key must not be empty
    ClientListCheckReturnTarget.ManageClientDetails.key must not be empty
    ClientListCheckReturnTarget.ChangeClientReference.key must not be empty
    ClientListCheckReturnTarget.RemoveClient.key must not be empty
  }

  "ClientListCheckPolicy values are distinct" in {
    Set[ClientListCheckPolicy](
      ClientListCheckPolicy.GroupA,
      ClientListCheckPolicy.GroupB,
      ClientListCheckPolicy.Exempt
    ).size mustBe 3
  }
}
