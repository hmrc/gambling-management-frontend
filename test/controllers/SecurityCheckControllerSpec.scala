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

package controllers

import base.SpecBase
import controllers.actions.FakeAgentIdentifierAction
import models.agent.ClientListStatus
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.SecurityCheckView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SecurityCheckControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = stubMessagesControllerComponents()
  private val view        = app.injector.instanceOf[SecurityCheckView]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]

  private class StubService(poll: ClientListStatus) extends GamblingService(null) {
    override def getClientListStatus(using HeaderCarrier): Future[ClientListStatus] = Future.successful(poll)
  }

  private def controller(poll: ClientListStatus = ClientListStatus.InProgress) =
    new SecurityCheckController(
      mcc.messagesApi,
      new FakeAgentIdentifierAction(bodyParsers),
      new StubService(poll),
      mcc,
      view
    )

  "onClientListCheck" - {
    "renders the spinner with a poll Refresh header for a valid return target" in {
      val result = controller().onClientListCheck("client-list", None)(FakeRequest())
      status(result) mustBe OK
      header("Refresh", result).value must include(
        routes.SecurityCheckController.pollClientListCheck("client-list", None, 0).url
      )
    }

    "redirects to system error for an unknown return target" in {
      val result = controller().onClientListCheck("nope", None)(FakeRequest())
      redirectLocation(result).value mustBe routes.SystemErrorController.onPageLoad().url
    }
  }

  "pollClientListCheck" - {
    "redirects to the resolved destination when the status succeeds" in {
      val result =
        controller(ClientListStatus.Succeeded).pollClientListCheck("agent-landing", Some("u1"), 0)(FakeRequest())
      redirectLocation(result).value mustBe controllers.agent.routes.AgentLandingController.onPageLoad("u1").url
    }

    "refreshes when still in progress" in {
      val result = controller(ClientListStatus.InProgress).pollClientListCheck("client-list", None, 0)(FakeRequest())
      status(result) mustBe OK
      header("Refresh", result).value must include(
        routes.SecurityCheckController.pollClientListCheck("client-list", None, 1).url
      )
    }

    "redirects to system error on Failed" in {
      val result = controller(ClientListStatus.Failed).pollClientListCheck("client-list", None, 0)(FakeRequest())
      redirectLocation(result).value mustBe routes.SystemErrorController.onPageLoad().url
    }

    "redirects to system error when retries are exhausted" in {
      val result = controller(ClientListStatus.InProgress).pollClientListCheck("client-list", None, 2)(FakeRequest())
      redirectLocation(result).value mustBe routes.SystemErrorController.onPageLoad().url
    }
  }
}
