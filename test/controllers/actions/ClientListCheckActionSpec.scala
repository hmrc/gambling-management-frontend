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

import base.SpecBase
import models.agent.ClientListStatus
import models.requests.AuthorisedRequest
import play.api.mvc.{AnyContent, PlayBodyParsers}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientListCheckActionSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]

  private class StubService(result: => Future[ClientListStatus]) extends GamblingService(null) {
    override def startClientListRetrieval(using HeaderCarrier): Future[ClientListStatus] = result
  }

  private def agentAction(result: => Future[ClientListStatus]) =
    (new FakeAgentIdentifierAction(bodyParsers) andThen new DefaultClientListCheckAction(new StubService(result)))(
      (_: AuthorisedRequest[AnyContent]) => Ok("passed")
    )

  private def orgAction(result: => Future[ClientListStatus]) =
    (new FakeIdentifierAction(bodyParsers) andThen new DefaultClientListCheckAction(new StubService(result)))(
      (_: AuthorisedRequest[AnyContent]) => Ok("passed")
    )

  "ClientListCheckAction" - {

    "passes through for a non-agent without calling the backend" in {
      val result = orgAction(Future.failed(new RuntimeException("should not be called")))(FakeRequest("GET", "/foo"))
      status(result) mustBe OK
      contentAsString(result) mustBe "passed"
    }

    "proceeds for an agent when the client list is ready" in {
      val result = agentAction(Future.successful(ClientListStatus.Succeeded))(FakeRequest("GET", "/foo"))
      status(result) mustBe OK
      contentAsString(result) mustBe "passed"
    }

    "redirects an agent to the spinner (carrying the original url) when the retrieval is in progress" in {
      val result = agentAction(Future.successful(ClientListStatus.InProgress))(FakeRequest("GET", "/foo"))
      redirectLocation(result).value mustBe
        controllers.routes.SecurityCheckController.onClientListCheck(RedirectUrl("/foo")).url
    }

    "redirects an agent to the spinner when the backend still needs to initiate the download" in {
      val result = agentAction(Future.successful(ClientListStatus.InitiateDownload))(FakeRequest("GET", "/foo"))
      redirectLocation(result).value mustBe
        controllers.routes.SecurityCheckController.onClientListCheck(RedirectUrl("/foo")).url
    }

    "redirects an agent to system error when the retrieval has failed" in {
      val result = agentAction(Future.successful(ClientListStatus.Failed))(FakeRequest("GET", "/foo"))
      redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
    }

    "redirects an agent to system error when the backend call errors" in {
      val result = agentAction(Future.failed(new RuntimeException("boom")))(FakeRequest("GET", "/foo"))
      redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
    }
  }
}
