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
import models.agent.ClientListStatus
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.agent.RetrievingClientView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrievingClientControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = stubMessagesControllerComponents()
  private val view        = app.injector.instanceOf[RetrievingClientView]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]

  private class StubService(start: ClientListStatus, poll: ClientListStatus) extends GamblingService(null) {
    override def startClientListRetrieval(using HeaderCarrier): Future[ClientListStatus] = Future.successful(start)
    override def getClientListStatus(using HeaderCarrier): Future[ClientListStatus]      = Future.successful(poll)
  }

  private def controller(start: ClientListStatus, poll: ClientListStatus = ClientListStatus.InProgress) =
    new RetrievingClientController(
      mcc.messagesApi,
      mcc,
      new FakeAgentIdentifierAction(bodyParsers),
      new StubService(start, poll),
      view
    )

  "onPageLoad" - {
    "renders the spinner with a Refresh header to start" in {
      val result = controller(ClientListStatus.Succeeded).onPageLoad(FakeRequest())
      status(result) mustBe OK
      header("Refresh", result).value must include(routes.RetrievingClientController.start().url)
    }
  }

  "start" - {
    "redirects to the client list on Succeeded" in {
      val result = controller(ClientListStatus.Succeeded).start()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.ClientListSearchController.onPageLoad().url
    }

    "redirects to failed-to-retrieve on Failed" in {
      val result = controller(ClientListStatus.Failed).start()(FakeRequest())
      redirectLocation(result).value mustBe routes.FailedToRetrieveClientController.onPageLoad().url
    }

    "refreshes (polls) on InProgress" in {
      val result = controller(ClientListStatus.InProgress).start()(FakeRequest())
      status(result) mustBe OK
      header("Refresh", result).value must include(routes.RetrievingClientController.poll(1).url)
    }

    "redirects to system error on InitiateDownload" in {
      val result = controller(ClientListStatus.InitiateDownload).start()(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
    }
  }

  "poll" - {
    "redirects to the client list when the status succeeds" in {
      val result = controller(ClientListStatus.Succeeded, ClientListStatus.Succeeded).poll(1)(FakeRequest())
      redirectLocation(result).value mustBe routes.ClientListSearchController.onPageLoad().url
    }

    "redirects to failed-to-retrieve when retries are exhausted" in {
      val result = controller(ClientListStatus.InProgress, ClientListStatus.InProgress).poll(8)(FakeRequest())
      redirectLocation(result).value mustBe routes.FailedToRetrieveClientController.onPageLoad().url
    }
  }
}
