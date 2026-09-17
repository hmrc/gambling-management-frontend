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

package controllers.actions

import models.agent.ClientListStatus
import models.requests.AuthorisedRequest
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Call, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ClientListStatusGuardSpec extends AnyFreeSpec with Matchers with ScalaFutures with OptionValues with MockitoSugar {

  given ExecutionContext = ExecutionContext.global

  private val agentRequest =
    AuthorisedRequest(FakeRequest(), AffinityGroup.Agent, mgdRegNum = "", userId = "internal-id", isAgent = true)

  private val systemErrorUrl    = controllers.routes.SystemErrorController.onPageLoad().url
  private val agentLostAccessUrl = controllers.agent.routes.AgentLostAccessController.onPageLoad().url
  private val securityCheckCall  = Call("GET", "/security-check")

  private def guardWith(status: Future[ClientListStatus]): ClientListStatusGuard = {
    val service = mock[GamblingService]
    when(service.startClientListRetrieval(using any)).thenReturn(status)
    when(service.getClientListStatus(using any)).thenReturn(status)
    new ClientListStatusGuard(service)
  }

  // matches any HeaderCarrier
  private def any: HeaderCarrier = org.mockito.ArgumentMatchers.any[HeaderCarrier]()

  "checkGroupA" - {
    "returns None when the status is Succeeded" in {
      guardWith(Future.successful(ClientListStatus.Succeeded)).checkGroupA(agentRequest).futureValue mustBe None
    }

    "redirects to agent lost access for non-Succeeded statuses" in {
      Seq(ClientListStatus.InProgress, ClientListStatus.Failed, ClientListStatus.InitiateDownload).foreach { status =>
        val result = guardWith(Future.successful(status)).checkGroupA(agentRequest).futureValue.value
        redirectLocation(Future.successful(result)).value mustBe agentLostAccessUrl
      }
    }

    "redirects to system error when the service fails" in {
      val result = guardWith(Future.failed(new RuntimeException("boom"))).checkGroupA(agentRequest).futureValue.value
      redirectLocation(Future.successful(result)).value mustBe systemErrorUrl
    }
  }

  "groupB filter" - {
    // Exercise the filter via its public invokeBlock: the block runs only when the filter passes.
    def runFilter(status: Future[ClientListStatus]): Result =
      guardWith(status)
        .groupB(securityCheckCall)
        .invokeBlock(agentRequest, (_: AuthorisedRequest[?]) => Future.successful(Results.Ok("passed")))
        .futureValue

    "passes through to the block when Succeeded" in {
      val result = runFilter(Future.successful(ClientListStatus.Succeeded))
      status(Future.successful(result)) mustBe OK
      contentAsString(Future.successful(result)) mustBe "passed"
    }

    "redirects to the security-check call when InProgress" in {
      val result = runFilter(Future.successful(ClientListStatus.InProgress))
      redirectLocation(Future.successful(result)).value mustBe securityCheckCall.url
    }

    "redirects to system error when Failed or InitiateDownload" in {
      Seq(ClientListStatus.Failed, ClientListStatus.InitiateDownload).foreach { s =>
        val result = runFilter(Future.successful(s))
        redirectLocation(Future.successful(result)).value mustBe systemErrorUrl
      }
    }
  }
}
