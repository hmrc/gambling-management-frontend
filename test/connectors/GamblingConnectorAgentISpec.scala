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

package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.agent.{AgentClient, AgentClientData, ClientListStatus, UpdateAgentClientRequest}
import models.requests.RemoveAgentClientRequest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext

class GamblingConnectorAgentISpec
    extends AsyncWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with IntegrationPatience {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  private val wireMockServer = new WireMockServer(0)

  override def beforeAll(): Unit = {
    wireMockServer.start()
    configureFor("localhost", wireMockServer.port())
  }

  override def afterAll(): Unit = wireMockServer.stop()

  private lazy val app =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.gambling.host" -> "localhost",
        "microservice.services.gambling.port" -> wireMockServer.port()
      )
      .build()

  private lazy val connector = app.injector.instanceOf[GamblingConnector]

  "startClientList" should {
    "parse the result field" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/gambling/agent/client-list/mgd/retrieval/start"))
          .willReturn(okJson(Json.obj("result" -> "succeeded").toString()))
      )
      connector.startClientList.map(_.result mustBe ClientListStatus.Succeeded)
    }
  }

  "getClientListStatus" should {
    "parse the result field" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/gambling/agent/client-list/mgd/retrieval/status"))
          .willReturn(okJson(Json.obj("result" -> "in-progress").toString()))
      )
      connector.getClientListStatus.map(_.result mustBe ClientListStatus.InProgress)
    }
  }

  "hasClient" should {
    "parse the hasClient boolean" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/gambling/agent/has-client/MGD/RN1"))
          .willReturn(okJson(Json.obj("hasClient" -> true).toString()))
      )
      connector.hasClient("MGD", "RN1").map(_.hasClient mustBe true)
    }
  }

  "getAllClients" should {
    "parse the clients array" in {
      val clientsJson = Json.obj(
        "clients" -> Json.arr(
          Json.obj(
            "regNumber"   -> "RN1",
            "clientName"  -> "Acme",
            "agentOwnRef" -> "ref"
          )
        )
      )
      wireMockServer.stubFor(
        get(urlEqualTo("/gambling/agent/client-list/mgd"))
          .willReturn(okJson(clientsJson.toString()))
      )
      connector.getAllClients.map { clients =>
        clients mustBe List(AgentClient("mgd", "RN1", Some("Acme"), Some("ref")))
      }
    }

    "fail on a non-200 response" in {
      wireMockServer.stubFor(
        get(urlEqualTo("/gambling/agent/client-list/mgd"))
          .willReturn(serverError())
      )
      recoverToSucceededIf[UpstreamErrorResponse](connector.getAllClients)
    }
  }

  "updateClient" should {
    "complete on 204" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/gambling/agent/update-client")).willReturn(aResponse().withStatus(204))
      )
      connector.updateClient(UpdateAgentClientRequest("mgd", "RN1", "ref")).map(_ => succeed)
    }

    "fail on a non-204 response" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/gambling/agent/update-client")).willReturn(serverError())
      )
      recoverToSucceededIf[UpstreamErrorResponse](connector.updateClient(UpdateAgentClientRequest("mgd", "RN1", "ref")))
    }
  }

  "removeClient" should {
    "complete on 204" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/gambling/agent/remove-client")).willReturn(aResponse().withStatus(204))
      )
      connector.removeClient(RemoveAgentClientRequest("mgd", "RN1")).map(_ => succeed)
    }
  }

  "saveAgentClient" should {
    "complete on 200" in {
      wireMockServer.stubFor(
        post(urlEqualTo("/gambling/user-cache/agent-client/user-1")).willReturn(okJson("{}"))
      )
      connector.saveAgentClient("user-1", AgentClientData("u1", "mgd", "RN1", Some("Acme"))).map(_ => succeed)
    }
  }
}
