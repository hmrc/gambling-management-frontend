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
import models.{GamblingError, ReturnSummary, ReturnSummaryError, UserAnswers}
import models.agent.{AgentClient, AgentDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ManageMachineGamesDutyControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val view        = app.injector.instanceOf[views.html.agent.ManageMachineGamesDutyView]

  private val client  = AgentClient("mgd", "XWM00000001770", Some("Client Business"), None)
  private val details = AgentDetails(Some("Agent 1"), None, None, None, None, None, None, None, None, None, None, None)

  private val withClient =
    UserAnswers("internal-id")
      .set(AgentClientsPage, List(client))
      .flatMap(_.set(SelectedClientPage, client.regNumber))
      .get

  private def controller(
    ua: UserAnswers,
    identify: AuthorisedAction = new FakeAgentIdentifierAction(bodyParsers),
    agentDetails: Either[GamblingError, AgentDetails] = Right(details),
    summary: Either[ReturnSummaryError, ReturnSummary] = Right(ReturnSummary(client.regNumber, 1, 4))
  ) = {
    val service = org.mockito.Mockito.mock(classOf[GamblingService])
    when(service.getAgentDetails(using any())).thenReturn(Future.successful(agentDetails))
    when(service.getReturnSummary(any())(using any())).thenReturn(Future.successful(summary))
    new ManageMachineGamesDutyController(
      mcc.messagesApi,
      identify,
      new PassThroughClientListCheckAction(),
      new FakeDataRetrievalAction(Some(ua)),
      new DataRequiredActionImpl(),
      new PassThroughHasClientGuard(service, null, null),
      service,
      mcc,
      view
    )
  }

  "onPageLoad" - {
    "renders the agent, the selected client and the overdue returns" in {
      val result = controller(withClient).onPageLoad()(FakeRequest())
      status(result) mustBe OK
      val doc    = Jsoup.parse(contentAsString(result))
      doc
        .select(".govuk-grid-column-full p.govuk-body")
        .first()
        .text() mustBe "Agent 1 acting on behalf of Client Business"
      doc.select(".card-body p").last().text() mustBe "You have 4 overdue returns."
    }

    "renders the page without the agent line or Client details card for an organisation" in {
      val result =
        controller(UserAnswers("id"), identify = new FakeIdentifierAction(bodyParsers)).onPageLoad()(FakeRequest())
      status(result) mustBe OK
      val doc    = Jsoup.parse(contentAsString(result))
      doc.select(".card-heading").eachText().toArray mustBe Array("Statement", "Returns", "Registration")
      doc.text must not include "acting on behalf of"
    }

    "redirects to JourneyRecovery when no client is selected" in {
      val ua     = UserAnswers("internal-id").set(AgentClientsPage, List(client)).get
      val result = controller(ua).onPageLoad()(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }

    "redirects to SystemError when the agent details cannot be retrieved" in {
      val result = controller(withClient, agentDetails = Left(GamblingError.UpstreamError)).onPageLoad()(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
    }

    "redirects to SystemError when the return summary cannot be retrieved" in {
      val result =
        controller(withClient, summary = Left(ReturnSummaryError.UpstreamError)).onPageLoad()(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
    }
  }
}
