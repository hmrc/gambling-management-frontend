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

package controllers.agent

import base.SpecBase
import controllers.actions.*
import models.UserAnswers
import models.agent.AgentClient
import navigation.ClientListCheckNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.AgentClientsPage
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{AuditService, GamblingService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentLandingControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val view        = app.injector.instanceOf[views.html.agent.AgentLandingView]

  private val client = AgentClient("u1", "mgd", "XMM00000000123", Some("Acme Casinos"), Some("ref"))

  private def controller(ua: UserAnswers) = {
    val repo  = org.mockito.Mockito.mock(classOf[SessionRepository])
    val audit = org.mockito.Mockito.mock(classOf[AuditService])
    when(repo.set(any)).thenReturn(Future.successful(true))
    when(audit.sendEvent(any)(using any, any)).thenReturn(Future.successful(AuditResult.Success))
    new AgentLandingController(
      mcc.messagesApi,
      new FakeAgentIdentifierAction(bodyParsers),
      new PassThroughStatusGuard(new GamblingService(null)),
      new ClientListCheckNavigator(),
      new FakeDataRetrievalAction(Some(ua)),
      new DataRequiredActionImpl(),
      new PassThroughHasClientGuard(new GamblingService(null), null, null),
      audit,
      repo,
      mcc,
      view
    )
  }

  "onPageLoad" - {
    "selects the client and renders the landing page" in {
      val ua     = UserAnswers("internal-id").set(AgentClientsPage, List(client)).get
      val result = controller(ua).onPageLoad("u1")(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) must (include("Acme Casinos") and include("XMM00000000123"))
    }

    "redirects to JourneyRecovery for an unknown client" in {
      val ua     = UserAnswers("internal-id").set(AgentClientsPage, List(client)).get
      val result = controller(ua).onPageLoad("missing")(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }
}
