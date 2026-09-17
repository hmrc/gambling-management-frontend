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
  private val client  = AgentClient("u1", regime = "MGD", regNumber = "RN1", clientName = Some("Acme"), agentOwnRef = Some("ref"))
  private val request =
    AuthorisedRequest(FakeRequest(), AffinityGroup.Agent, mgdRegNum = "", userId = userId, isAgent = true)

  private val systemErrorUrl = controllers.routes.SystemErrorController.onPageLoad().url

  private def userAnswersWithSelectedClient: UserAnswers =
    UserAnswers(userId)
      .set(AgentClientsPage, List(client))
      .flatMap(_.set(SelectedClientPage, "u1"))
      .get

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

    "redirects to system error when no client is selected" in {
      val (guard, repo, _, _) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(UserAnswers(userId))))
      redirectLocation(Future.successful(guard.check(request).futureValue.value)).value mustBe systemErrorUrl
    }

    "returns None when the agent still has the client" in {
      val (guard, repo, service, _) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(userAnswersWithSelectedClient)))
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(true))

      guard.check(request).futureValue mustBe None
    }

    "audits and redirects to system error when the agent no longer has the client" in {
      val (guard, repo, service, audit) = newGuard()
      when(repo.get(userId)).thenReturn(Future.successful(Some(userAnswersWithSelectedClient)))
      when(service.hasClient(anyString, anyString)(using any[HeaderCarrier])).thenReturn(Future.successful(false))
      when(audit.sendEvent(any)(using any, any)).thenReturn(Future.successful(AuditResult.Success))

      redirectLocation(Future.successful(guard.check(request).futureValue.value)).value mustBe systemErrorUrl
      verify(audit).sendEvent(any)(using any, any)
    }
  }

  "forInstanceId" - {

    "passes through (None) for a non-agent request without calling the backend" in {
      val (guard, _, service, _) = newGuard()
      val orgRequest             = models.requests.DataRequest(FakeRequest(), userId, UserAnswers(userId), isAgent = false)

      guard.checkForInstanceId(orgRequest, "u1").futureValue mustBe None
      verify(service, never).hasClient(anyString, anyString)(using any[HeaderCarrier])
    }
  }
}
