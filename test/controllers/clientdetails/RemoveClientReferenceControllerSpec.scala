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
import services.{GamblingService, ManageService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveClientReferenceControllerSpec extends SpecBase {

  private val app = applicationBuilder().build()

  private val mcc = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]

  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]

  private val view = app.injector.instanceOf[views.html.clientdetails.RemoveClientReferenceView]

  private val client =
    AgentClient(
      regime = "mgd",
      regNumber = "RN1",
      clientName = Some("Acme Casinos"),
      agentOwnRef = Some("CLIENT-REF-123")
    )

  private val clientWithNoneAgentRef =
    AgentClient(
      regime = "mgd",
      regNumber = "RN2",
      clientName = Some("No Reference Casinos"),
      agentOwnRef = None
    )

  private val clientWithSpacesAgentRef =
    AgentClient(
      regime = "mgd",
      regNumber = "RN2",
      clientName = Some("No Reference Casinos"),
      agentOwnRef = Some("       ")
    )

  private val ua: UserAnswers = UserAnswers("internal-id").set(AgentClientsPage, List(client)).get

  private val uaWithNoneAgentRef: UserAnswers =
    UserAnswers("internal-id").set(AgentClientsPage, List(clientWithNoneAgentRef)).get

  private val uaWithSpacesAgentRef: UserAnswers =
    UserAnswers("internal-id").set(AgentClientsPage, List(clientWithSpacesAgentRef)).get

  private class StubManageService extends ManageService(null, null) {
    override def updateClient(regNumber: String, ua: UserAnswers, clientRef: String)(using
      HeaderCarrier
    ): Future[Unit] = Future.unit
  }

  private def controller(userAnswers: UserAnswers = ua) =
    new RemoveClientReferenceController(
      mcc.messagesApi,
      new FakeAgentIdentifierAction(bodyParsers),
      new PassThroughClientListCheckAction(),
      new FakeDataRetrievalAction(Some(userAnswers)),
      new DataRequiredActionImpl(),
      new PassThroughHasClientGuard(new GamblingService(null), null, null),
      new RemoveClientReferenceFormProvider(),
      new StubManageService(),
      mcc,
      view
    )

  "onPageLoad" - {

    "renders the confirmation page for a known client" in {
      val result = controller().onPageLoad("RN1")(addCSRFToken(FakeRequest()))
      status(result) mustBe OK
      contentAsString(result) must include("CLIENT-REF-123")
    }

    "renders the confirmation page when the client agent reference is None" in {
      val result = controller(uaWithNoneAgentRef).onPageLoad("RN2")(addCSRFToken(FakeRequest()))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.clientdetails.routes.ManageClientDetailsController
        .onPageLoad()
        .url
    }

    "renders the confirmation page when the client agent reference is Spaces" in {
      val result = controller(uaWithSpacesAgentRef).onPageLoad("RN2")(addCSRFToken(FakeRequest()))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.clientdetails.routes.ManageClientDetailsController
        .onPageLoad()
        .url
    }

    "redirects to JourneyRecovery for an unknown client" in {
      val result = controller().onPageLoad("missing")(addCSRFToken(FakeRequest()))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }

  "onSubmit" - {

    "redirects to manage-client-details when Yes" in {

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
        controllers.clientdetails.routes.ManageClientDetailsController
          .onPageLoad()
          .url
    }

    "redirects to manage-client-details when No" in {

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
        controllers.clientdetails.routes.ManageClientDetailsController
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
        controller(uaWithNoneAgentRef)
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
