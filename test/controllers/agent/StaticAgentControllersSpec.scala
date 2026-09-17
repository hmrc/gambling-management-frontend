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
import controllers.actions.FakeAgentIdentifierAction
import controllers.clientdetails.{ClientRefUpdateConfirmationController, ClientRemovedController}
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class StaticAgentControllersSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = stubMessagesControllerComponents()
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private def auth        = new FakeAgentIdentifierAction(bodyParsers)

  "AgentLostAccessController.onPageLoad returns OK" in {
    val controller = new AgentLostAccessController(mcc.messagesApi, mcc, app.injector.instanceOf[views.html.agent.AgentLostAccessView])
    status(controller.onPageLoad(FakeRequest())) mustBe OK
  }

  "FailedToRetrieveClientController.onPageLoad returns OK" in {
    val controller = new FailedToRetrieveClientController(mcc.messagesApi, mcc, auth, app.injector.instanceOf[views.html.agent.FailedToRetrieveClientView])
    status(controller.onPageLoad(FakeRequest())) mustBe OK
  }

  "NoAuthorisedClientsController.onPageLoad returns OK" in {
    val controller = new NoAuthorisedClientsController(mcc.messagesApi, mcc, auth, app.injector.instanceOf[views.html.agent.NoAuthorisedClientsView])
    status(controller.onPageLoad(FakeRequest())) mustBe OK
  }

  "ClientRemovedController.onPageLoad returns OK" in {
    val controller = new ClientRemovedController(mcc.messagesApi, mcc, auth, app.injector.instanceOf[views.html.clientdetails.ClientRemovedView])
    status(controller.onPageLoad(FakeRequest())) mustBe OK
  }

  "ClientRefUpdateConfirmationController.onPageLoad returns OK" in {
    val controller = new ClientRefUpdateConfirmationController(mcc.messagesApi, mcc, auth, app.injector.instanceOf[views.html.clientdetails.ClientRefUpdateConfirmationView])
    status(controller.onPageLoad(FakeRequest())) mustBe OK
  }
}
