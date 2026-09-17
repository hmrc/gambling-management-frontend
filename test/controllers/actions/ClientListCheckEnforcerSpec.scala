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

import models.agent.ClientListCheckPolicy
import models.requests.AuthorisedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{RequestHeader, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup

import scala.concurrent.{ExecutionContext, Future}

class ClientListCheckEnforcerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  given ExecutionContext = ExecutionContext.global

  private def agentRequest(isAgent: Boolean = true) =
    AuthorisedRequest(FakeRequest(), AffinityGroup.Agent, mgdRegNum = "", userId = "id", isAgent = isAgent)

  private val block: AuthorisedRequest[?] => Future[Result] =
    _ => Future.successful(Results.Ok("passed"))

  private val guardRedirect = Results.Redirect(controllers.routes.SystemErrorController.onPageLoad())

  private def enforcerWith(
    policy: ClientListCheckPolicy,
    centralHasClient: Boolean = true,
    groupAStatus: Option[Result] = None,
    hasClientResult: Option[Result] = None
  ): ClientListCheckEnforcer = {
    val resolver   = mock[ClientListCheckPolicyResolver]
    val statusGuard = mock[ClientListStatusGuard]
    val hasClient  = mock[HasClientGuard]

    when(resolver.resolve(any[RequestHeader])).thenReturn(policy)
    when(resolver.shouldRunCentralHasClient(any[RequestHeader])).thenReturn(centralHasClient)
    when(statusGuard.checkGroupA(any)).thenReturn(Future.successful(groupAStatus))
    when(hasClient.check(any)).thenReturn(Future.successful(hasClientResult))

    new ClientListCheckEnforcer(resolver, statusGuard, hasClient)
  }

  "ClientListCheckEnforcer" - {

    "runs the block directly for a non-agent request" in {
      val enforcer = enforcerWith(ClientListCheckPolicy.GroupA, groupAStatus = Some(guardRedirect))
      val result   = enforcer(agentRequest(isAgent = false))(block).futureValue
      contentAsString(Future.successful(result)) mustBe "passed"
    }

    "runs the block for GroupB and Exempt policies" in {
      Seq(ClientListCheckPolicy.GroupB, ClientListCheckPolicy.Exempt).foreach { policy =>
        val result = enforcerWith(policy)(agentRequest())(block).futureValue
        contentAsString(Future.successful(result)) mustBe "passed"
      }
    }

    "returns the status-guard result for GroupA when the status check fails" in {
      val enforcer = enforcerWith(ClientListCheckPolicy.GroupA, groupAStatus = Some(guardRedirect))
      val result   = enforcer(agentRequest())(block).futureValue
      status(Future.successful(result)) mustBe SEE_OTHER
    }

    "returns the has-client result for GroupA when status passes but has-client fails" in {
      val enforcer =
        enforcerWith(ClientListCheckPolicy.GroupA, groupAStatus = None, hasClientResult = Some(guardRedirect))
      val result = enforcer(agentRequest())(block).futureValue
      status(Future.successful(result)) mustBe SEE_OTHER
    }

    "runs the block for GroupA when status and has-client both pass" in {
      val enforcer = enforcerWith(ClientListCheckPolicy.GroupA, groupAStatus = None, hasClientResult = None)
      val result   = enforcer(agentRequest())(block).futureValue
      contentAsString(Future.successful(result)) mustBe "passed"
    }

    "runs the block for GroupA when central has-client is skipped for the route" in {
      val enforcer =
        enforcerWith(ClientListCheckPolicy.GroupA, centralHasClient = false, groupAStatus = None)
      val result = enforcer(agentRequest())(block).futureValue
      contentAsString(Future.successful(result)) mustBe "passed"
    }
  }
}
