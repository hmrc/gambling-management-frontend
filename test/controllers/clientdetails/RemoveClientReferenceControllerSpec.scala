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

package controllers.clientdetails

import base.SpecBase
import controllers.actions.*
import forms.clientdetails.RemoveClientReferenceFormProvider
import models.UserAnswers
import models.agent.AgentClient
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.AgentClientsPage
import play.api.mvc.PlayBodyParsers
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.GamblingService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveClientReferenceControllerSpec extends SpecBase {

  private val app =
    applicationBuilder().build()

  private val mcc =
    app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]

  private val bodyParsers =
    app.injector.instanceOf[PlayBodyParsers]

  private val view =
    app.injector.instanceOf[
      views.html.clientdetails.RemoveClientReferenceView
    ]

  private val client =
    AgentClient(
      regime = "mgd",
      regNumber = "RN1",
      clientName = Some("Acme Casinos"),
      agentOwnRef = Some("CLIENT-REF-123")
    )

  private val clientWithoutAgentRef =
    AgentClient(
      regime = "mgd",
      regNumber = "RN2",
      clientName = Some("No Reference Casinos"),
      agentOwnRef = None
    )

  private val ua: UserAnswers =
    UserAnswers("internal-id")
      .set(
        AgentClientsPage,
        List(client)
      )
      .get

  private val uaWithoutAgentRef: UserAnswers =
    UserAnswers("internal-id")
      .set(
        AgentClientsPage,
        List(clientWithoutAgentRef)
      )
      .get

  private def controller(
    userAnswers: UserAnswers = ua
  ) = {

    val repo =
      org.mockito.Mockito.mock(
        classOf[SessionRepository]
      )

    when(repo.set(any))
      .thenReturn(
        Future.successful(true)
      )

    new RemoveClientReferenceController(
      mcc.messagesApi,
      repo,
      new FakeAgentIdentifierAction(bodyParsers),
      new PassThroughClientListCheckAction(),
      new FakeDataRetrievalAction(Some(userAnswers)),
      new DataRequiredActionImpl(),
      new PassThroughHasClientGuard(
        new GamblingService(null),
        null,
        null
      ),
      new RemoveClientReferenceFormProvider(),
      mcc,
      view
    )
  }

  "onPageLoad" - {

    "renders the confirmation page for a known client" in {

      val result =
        controller()
          .onPageLoad("RN1")(
            addCSRFToken(FakeRequest())
          )

      status(result) mustBe OK

      contentAsString(result) must include(
        "CLIENT-REF-123"
      )
    }

    "renders the confirmation page when the client has no agent reference" in {

      val result =
        controller(uaWithoutAgentRef)
          .onPageLoad("RN2")(
            addCSRFToken(FakeRequest())
          )

      status(result) mustBe OK
    }

    "redirects to JourneyRecovery for an unknown client" in {

      val result =
        controller()
          .onPageLoad("missing")(
            addCSRFToken(FakeRequest())
          )

      status(result) mustBe SEE_OTHER

      redirectLocation(result).value mustBe
        controllers.routes.JourneyRecoveryController
          .onPageLoad()
          .url
    }
  }

  "onSubmit" - {

    "redirects to search-for-a-client when Yes" in {

      val request =
        FakeRequest()
          .withFormUrlEncodedBody(
            "value" -> "true"
          )

      val result =
        controller()
          .onSubmit("RN1")(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).value mustBe
        controllers.agent.routes.ClientListController
          .onPageLoad()
          .url
    }

    "redirects to search-for-a-client when No" in {

      val request =
        FakeRequest()
          .withFormUrlEncodedBody(
            "value" -> "false"
          )

      val result =
        controller()
          .onSubmit("RN1")(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).value mustBe
        controllers.agent.routes.ClientListController
          .onPageLoad()
          .url
    }

    "returns BadRequest when nothing is selected" in {

      val request =
        addCSRFToken(
          FakeRequest()
            .withFormUrlEncodedBody()
        )

      val result =
        controller()
          .onSubmit("RN1")(request)

      status(result) mustBe BAD_REQUEST

      contentAsString(result) must include(
        "CLIENT-REF-123"
      )
    }

    "returns BadRequest when nothing is selected and the client has no agent reference" in {

      val request =
        addCSRFToken(
          FakeRequest()
            .withFormUrlEncodedBody()
        )

      val result =
        controller(uaWithoutAgentRef)
          .onSubmit("RN2")(request)

      status(result) mustBe BAD_REQUEST
    }

    "redirects to JourneyRecovery for an unknown client" in {

      val request =
        FakeRequest()
          .withFormUrlEncodedBody(
            "value" -> "true"
          )

      val result =
        controller()
          .onSubmit("missing")(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result).value mustBe
        controllers.routes.JourneyRecoveryController
          .onPageLoad()
          .url
    }
  }
}
