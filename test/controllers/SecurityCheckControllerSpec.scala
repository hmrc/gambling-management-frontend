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
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.SecurityCheckView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SecurityCheckControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = stubMessagesControllerComponents()
  private val view        = app.injector.instanceOf[SecurityCheckView]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]

  private val returnUrl = "/agent/manage-client"

  // interval 5s, max wait 45s => 9 polls before giving up
  private val maxRetries = 9

  private class StubService(poll: ClientListStatus) extends GamblingService(null) {
    override def getClientListStatus(using HeaderCarrier): Future[ClientListStatus] = Future.successful(poll)
  }

  private def controller(poll: ClientListStatus = ClientListStatus.InProgress) =
    new SecurityCheckController(
      mcc.messagesApi,
      new FakeAgentIdentifierAction(bodyParsers),
      new StubService(poll),
      testAppConfig,
      mcc,
      view
    )

  "onClientListCheck" - {
    "renders the spinner with a poll Refresh header for a valid relative return url" in {
      val result = controller().onClientListCheck(RedirectUrl(returnUrl))(FakeRequest())
      status(result) mustBe OK
      header("Refresh", result).value must include(
        routes.SecurityCheckController.pollClientListCheck(RedirectUrl(returnUrl), 0).url
      )
    }

    "redirects to system error for a non-relative return url" in {
      val result = controller().onClientListCheck(RedirectUrl("https://evil.example.com"))(FakeRequest())
      redirectLocation(result).value mustBe routes.SystemErrorController.onPageLoad().url
    }
  }

  "pollClientListCheck" - {
    "redirects to the original return url when the status succeeds" in {
      val result =
        controller(ClientListStatus.Succeeded).pollClientListCheck(RedirectUrl(returnUrl), 0)(FakeRequest())
      redirectLocation(result).value mustBe returnUrl
    }

    "refreshes when still in progress" in {
      val result = controller(ClientListStatus.InProgress).pollClientListCheck(RedirectUrl(returnUrl), 0)(FakeRequest())
      status(result) mustBe OK
      header("Refresh", result).value must include(
        routes.SecurityCheckController.pollClientListCheck(RedirectUrl(returnUrl), 1).url
      )
    }

    "keeps polling when the backend still needs to initiate the download" in {
      val result =
        controller(ClientListStatus.InitiateDownload).pollClientListCheck(RedirectUrl(returnUrl), 0)(FakeRequest())
      status(result) mustBe OK
      header("Refresh", result).value must include(
        routes.SecurityCheckController.pollClientListCheck(RedirectUrl(returnUrl), 1).url
      )
    }

    "redirects to system error on Failed" in {
      val result = controller(ClientListStatus.Failed).pollClientListCheck(RedirectUrl(returnUrl), 0)(FakeRequest())
      redirectLocation(result).value mustBe routes.SystemErrorController.onPageLoad().url
    }

    "redirects to system error when the max wait is exceeded" in {
      val result =
        controller(ClientListStatus.InProgress).pollClientListCheck(RedirectUrl(returnUrl), maxRetries)(FakeRequest())
      redirectLocation(result).value mustBe routes.SystemErrorController.onPageLoad().url
    }

    "redirects to system error for a non-relative return url" in {
      val result =
        controller(ClientListStatus.Succeeded).pollClientListCheck(RedirectUrl("https://evil.example.com"), 0)(
          FakeRequest()
        )
      redirectLocation(result).value mustBe routes.SystemErrorController.onPageLoad().url
    }
  }
}
