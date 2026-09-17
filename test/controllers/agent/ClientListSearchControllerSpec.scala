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
import controllers.actions.{FakeAgentIdentifierAction, FakeDataRetrievalAction, PassThroughStatusGuard}
import models.UserAnswers
import models.agent.AgentClient
import navigation.ClientListCheckNavigator
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{GamblingService, ManageService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.agent.ClientListSearchView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientListSearchControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = stubMessagesControllerComponents()
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val view        = app.injector.instanceOf[ClientListSearchView]
  private val navigator   = new ClientListCheckNavigator()
  private val statusGuard = new PassThroughStatusGuard(new GamblingService(null))

  private val client = AgentClient("u1", "mgd", "RN1", Some("Acme Casinos"), Some("ref1"))

  private class StubManageService(clients: List[AgentClient])
      extends ManageService(null, null) {
    override def resolveAndStoreAgentClients(ua: UserAnswers)(using HeaderCarrier): Future[(List[AgentClient], UserAnswers)] =
      Future.successful((clients, ua))
  }

  private def controller(clients: List[AgentClient]) =
    new ClientListSearchController(
      mcc.messagesApi,
      new FakeAgentIdentifierAction(bodyParsers),
      statusGuard,
      navigator,
      new FakeDataRetrievalAction(Some(emptyUserAnswers)),
      new StubManageService(clients),
      mcc,
      view
    )

  "onPageLoad" - {
    "renders the client list when clients are returned" in {
      val result = controller(List(client)).onPageLoad()(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) must include("Acme Casinos")
    }

    "redirects to no-authorised-clients when the list is empty" in {
      val result = controller(Nil).onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.NoAuthorisedClientsController.onPageLoad().url
    }
  }
}
