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

import models.UserAnswers
import models.agent.AgentClient
import models.requests.AuthorisedRequest
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{AuditService, GamblingService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.{ExecutionContext, Future}

class HasClientGuardSpec extends AnyFreeSpec with Matchers with ScalaFutures with OptionValues with MockitoSugar {

  given ExecutionContext = ExecutionContext.global

  private val userId  = "internal-id"
  private val client  =
    AgentClient(regime = "MGD", regNumber = "RegNumber1", clientName = Some("Acme"), agentOwnRef = Some("ref"))
  private val request =
    AuthorisedRequest(FakeRequest(), AffinityGroup.Agent, mgdRegNum = "", userId = userId, isAgent = true)

  private val systemErrorUrl     = controllers.routes.SystemErrorController.onPageLoad().url
  private val agentLostAccessUrl = controllers.agent.routes.AgentLostAccessController.onPageLoad().url

  private def userAnswersWithClientList: UserAnswers =
    UserAnswers(userId).set(AgentClientsPage, List(client)).get

  private def userAnswersWithSelectedClient: UserAnswers =
    userAnswersWithClientList.set(SelectedClientPage, "RegNumber1").get

  private def newGuard(
    sessionRepository: SessionRepository = mock[SessionRepository],
    service: GamblingService = mock[GamblingService],
    audit: AuditService = mock[AuditService]
  ): (HasClientGuard, SessionRepository, GamblingService, AuditService) =
    (new HasClientGuard(service, sessionRepository, audit), sessionRepository, service, audit)

  "check" - {

    "redirects to system error when there are no UserAnswers" in {
      val (guard, repo, _, _) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(None))
      redirectLocation(Future.successful(guard.check(request).futureValue.value)).value mustBe systemErrorUrl
    }

    "redirects to system error when no client is selected in UserAnswers" in {
      val (guard, repo, _, _) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(userAnswersWithClientList)))
      redirectLocation(Future.successful(guard.check(request).futureValue.value)).value mustBe systemErrorUrl
    }

    "redirects to system error when RegNumber is not found in the Client List" in {
      val uaMissingClient = userAnswersWithClientList.set(SelectedClientPage, "RegNumber-not-in-Client-List").get

      val (guard, repo, service, _) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(uaMissingClient)))
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(true))

      redirectLocation(Future.successful(guard.check(request).futureValue.value)).value mustBe systemErrorUrl
    }

    "returns None when the agent is still authorised for the client (hasClient returns true)" in {
      val (guard, repo, service, _) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(userAnswersWithSelectedClient)))
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(true))

      guard.check(request).futureValue mustBe None
    }

    "returns system error when the Client in the Client List has an empty regime" in {
      val uaMissingRegime = UserAnswers(userId)
        .set(
          AgentClientsPage,
          List(AgentClient(regime = "", regNumber = "RegNumber1", clientName = Some("Acme"), agentOwnRef = Some("ref")))
        )
        .flatMap(_.set(SelectedClientPage, "RegNumber1"))
        .get

      val (guard, repo, service, _) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(uaMissingRegime)))
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(true))

      redirectLocation(Future.successful(guard.check(request).futureValue.value)).value mustBe systemErrorUrl
    }

    "returns system error when the Client in the Client List has an empty regNumber" in {
      val uaMissingRegNumber = UserAnswers(userId)
        .set(
          AgentClientsPage,
          List(AgentClient(regime = "MGD", regNumber = "", clientName = Some("Acme"), agentOwnRef = Some("ref")))
        )
        .flatMap(_.set(SelectedClientPage, "RegNumber1"))
        .get

      val (guard, repo, service, _) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(uaMissingRegNumber)))
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(true))

      redirectLocation(Future.successful(guard.check(request).futureValue.value)).value mustBe systemErrorUrl
    }

    "audits and redirects to agent lost access when the agent is no longer authorised for the client (hasClient returns false)" in {
      val (guard, repo, service, audit) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(userAnswersWithSelectedClient)))
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(false))
      when(audit.sendEvent(any)(using any, any)).thenReturn(Future.successful(AuditResult.Success))

      redirectLocation(Future.successful(guard.check(request).futureValue.value)).value mustBe agentLostAccessUrl
      verify(audit).sendEvent(any)(using any, any)
    }
  }

  "forRegNumber" - {

    "passes through (None) for a non-agent request without calling the backend" in {
      val (guard, _, service, _) = newGuard()
      val orgRequest             = models.requests.DataRequest(FakeRequest(), userId, UserAnswers(userId), isAgent = false)

      guard.checkForRegNumber(orgRequest, "RegNumber1").futureValue mustBe None
      verify(service, never).hasClient(anyString, anyString)(using any[HeaderCarrier])
    }

    "passes through (None) for an agent who is still authorised for the client (hasClient returns true)" in {
      val (guard, _, service, _) = newGuard()
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(true))
      val request                = models.requests.DataRequest(FakeRequest(), userId, userAnswersWithSelectedClient, isAgent = true)

      guard.checkForRegNumber(request, "RegNumber1").futureValue mustBe None
    }

    "audits and redirects to agent lost access when the agent is no longer authorised for the client" +
      " (hasClient returns false) even though the client exists in their Client List" in {
        val (guard, _, service, audit) = newGuard()
        when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(false))
        when(audit.sendEvent(any)(using any, any)).thenReturn(Future.successful(AuditResult.Success))
        val request                    = models.requests.DataRequest(FakeRequest(), userId, userAnswersWithSelectedClient, isAgent = true)

        redirectLocation(
          Future.successful(guard.checkForRegNumber(request, "RegNumber1").futureValue.value)
        ).value mustBe agentLostAccessUrl
        verify(audit).sendEvent(any)(using any, any)
      }

    "redirects to system error when the client is not found in the agents Client List" in {
      val (guard, _, _, _) = newGuard()
      val request          = models.requests.DataRequest(FakeRequest(), userId, UserAnswers(userId), isAgent = true)

      redirectLocation(
        Future.successful(guard.checkForRegNumber(request, "missing-reg-number").futureValue.value)
      ).value mustBe systemErrorUrl
    }
  }

  "checkCurrentClient" - {

    "passes through (None) for a non-agent request without calling the backend" in {
      val (guard, _, service, _) = newGuard()
      val orgRequest             = models.requests.DataRequest(FakeRequest(), userId, UserAnswers(userId), isAgent = false)

      guard.checkCurrentClient(orgRequest).futureValue mustBe None
      verify(service, never).hasClient(anyString, anyString)(using any[HeaderCarrier])
    }

    "passes through (None) for an agent who has the client in their Client List" in {
      val (guard, _, service, _) = newGuard()
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(true))
      val request                = models.requests.DataRequest(FakeRequest(), userId, userAnswersWithSelectedClient, isAgent = true)

      guard.checkCurrentClient(request).futureValue mustBe None
    }

    "returns system error for an agent when no client is selected from their Client List" in {
      val (guard, _, service, _) = newGuard()
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(true))
      val request                = models.requests.DataRequest(FakeRequest(), userId, userAnswersWithClientList, isAgent = true)
      redirectLocation(
        Future.successful(guard.checkCurrentClient(request).futureValue.value)
      ).value mustBe systemErrorUrl
    }
  }
}
