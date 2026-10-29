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
import controllers.actions.{ClientListCheckAction, PassThroughClientListCheckAction}
import models.{GamblingError, UserAnswers}
import models.agent.{AgentClient, AgentDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{GamblingService, ManageService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ManageYourDetailsControllerSpec extends SpecBase {

  private def appWith(
    result: Either[GamblingError, AgentDetails],
    clients: Future[List[AgentClient]] = Future.successful(Nil),
    userAnswers: Option[UserAnswers] = Some(emptyUserAnswers)
  ) = {
    val service       = org.mockito.Mockito.mock(classOf[GamblingService])
    when(service.getAgentDetails(using any())).thenReturn(Future.successful(result))
    val manageService = org.mockito.Mockito.mock(classOf[ManageService])
    when(manageService.resolveAndStoreAgentClients(any())(using any()))
      .thenReturn(clients.map(_ -> emptyUserAnswers))
    applicationBuilder(userAnswers)
      .overrides(
        bind[GamblingService].toInstance(service),
        bind[ManageService].toInstance(manageService),
        bind[ClientListCheckAction].toInstance(new PassThroughClientListCheckAction())
      )
      .build()
  }

  private val details = AgentDetails(
    Some("Agent 1"),
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None
  )

  "ManageYourDetailsController" - {
    "must show the agent name" in {
      val app = appWith(Right(details))
      running(app) {
        val result = route(app, FakeRequest(GET, routes.ManageYourDetailsController.onPageLoad().url)).value
        status(result) mustEqual OK
        Jsoup.parse(contentAsString(result)).select("main p.govuk-body").first().text() mustBe "Agent 1"
      }
    }

    "must show the empty message and no client links when the client list is empty" in {
      val app = appWith(Right(details))
      running(app) {
        val result = route(app, FakeRequest(GET, routes.ManageYourDetailsController.onPageLoad().url)).value
        val doc    = Jsoup.parse(contentAsString(result))
        doc.select(".card-action").first().text() mustBe "You have no clients to view."
        doc.select(".card-action").first().select("a").size() mustBe 0
      }
    }

    "must show the client links when the agent has clients" in {
      val client = AgentClient("MGD", "RN1", Some("Client 1"), None)
      val app    = appWith(Right(details), Future.successful(List(client)))
      running(app) {
        val result = route(app, FakeRequest(GET, routes.ManageYourDetailsController.onPageLoad().url)).value
        Jsoup.parse(contentAsString(result)).select(".card-action").first().select("a").size() mustBe 2
      }
    }

    "must redirect to system error when the client list cannot be retrieved" in {
      val app = appWith(Right(details), Future.failed(new RuntimeException("boom")))
      running(app) {
        val result = route(app, FakeRequest(GET, routes.ManageYourDetailsController.onPageLoad().url)).value
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must show the page when the user has no user answers yet" in {
      val client = AgentClient("MGD", "RN1", Some("Client 1"), None)
      val app    = appWith(Right(details), Future.successful(List(client)), userAnswers = None)
      running(app) {
        val result = route(app, FakeRequest(GET, routes.ManageYourDetailsController.onPageLoad().url)).value
        status(result) mustEqual OK
      }
    }

    "must redirect an organisation to the access denied page (agent-only page)" in {
      val app = organisationDeniedApplicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(app) {
        val result = route(app, FakeRequest(GET, routes.ManageYourDetailsController.onPageLoad().url)).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.AccessDeniedController.onPageLoad().url
      }
    }

    "must redirect to page not found when details are missing" in {
      val app = appWith(Left(GamblingError.NotFound))
      running(app) {
        val result = route(app, FakeRequest(GET, routes.ManageYourDetailsController.onPageLoad().url)).value
        redirectLocation(result).value mustEqual controllers.routes.PageNotFoundController.onPageLoad().url
      }
    }

    "must redirect to system error on upstream failure" in {
      val app = appWith(Left(GamblingError.UnexpectedError))
      running(app) {
        val result = route(app, FakeRequest(GET, routes.ManageYourDetailsController.onPageLoad().url)).value
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
