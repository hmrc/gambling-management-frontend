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

package services

import connectors.GamblingConnector
import models.UserAnswers
import models.agent.{AgentClient, UpdateAgentClientRequest}
import models.requests.RemoveAgentClientRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.AgentClientsPage
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ManageServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  private given HeaderCarrier = HeaderCarrier()

  private val client = AgentClient("mgd", "RN1", Some("Acme"), Some("ref"))
  private val userId = "internal-id"

  private def newService = {
    val connector = mock[GamblingConnector]
    val repo      = mock[SessionRepository]
    (new ManageService(connector, repo), connector, repo)
  }

  "resolveAndStoreAgentClients" should {
    "return the cached client list without calling the backend" in {
      val (service, connector, _) = newService
      val ua                      = UserAnswers(userId).set(AgentClientsPage, List(client)).get

      service.resolveAndStoreAgentClients(ua).futureValue._1 mustBe List(client)
      verify(connector, org.mockito.Mockito.never).getAllClients(using any)
    }

    "fetch from the backend and cache on a miss" in {
      val (service, connector, repo) = newService
      when(connector.getAllClients(using any)).thenReturn(Future.successful(List(client)))
      when(repo.set(any)).thenReturn(Future.successful(true))

      val (clients, ua) = service.resolveAndStoreAgentClients(UserAnswers(userId)).futureValue
      clients mustBe List(client)
      ua.get(AgentClientsPage) mustBe Some(List(client))
      verify(repo).set(any)
    }
  }

  "updateClient" should {
    "send an update for the resolved client" in {
      val (service, connector, _) = newService
      val ua                      = UserAnswers(userId).set(AgentClientsPage, List(client)).get
      when(connector.updateClient(any[UpdateAgentClientRequest])(using any)).thenReturn(Future.unit)

      service.updateClient("RN1", ua, "newref").futureValue
      verify(connector).updateClient(eqTo(UpdateAgentClientRequest("mgd", "RN1", "newref")))(using any)
    }

    "fail when the client is not in the cache" in {
      val (service, _, _) = newService
      service.updateClient("missing", UserAnswers(userId), "x").failed.futureValue mustBe a[RuntimeException]
    }
  }

  "removeClient" should {
    "send a remove for the resolved client" in {
      val (service, connector, _) = newService
      val ua                      = UserAnswers(userId).set(AgentClientsPage, List(client)).get
      when(connector.removeClient(any[RemoveAgentClientRequest])(using any)).thenReturn(Future.unit)

      service.removeClient("RN1", ua).futureValue
      verify(connector).removeClient(eqTo(RemoveAgentClientRequest("mgd", "RN1")))(using any)
    }
  }
}
