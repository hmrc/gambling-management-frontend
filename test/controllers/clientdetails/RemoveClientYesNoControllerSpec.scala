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
import forms.clientdetails.RemoveClientYesNoFormProvider
import models.UserAnswers
import models.agent.AgentClient
import navigation.ClientListCheckNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.mvc.PlayBodyParsers
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{GamblingService, ManageService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveClientYesNoControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val view        = app.injector.instanceOf[views.html.clientdetails.RemoveClientYesNoView]

  private val client          = AgentClient("u1", "mgd", "RN1", Some("Acme Casinos"), Some("ref"))
  private val ua: UserAnswers =
    UserAnswers("internal-id").set(AgentClientsPage, List(client)).flatMap(_.set(SelectedClientPage, "u1")).get

  private class StubManageService extends ManageService(null, null) {
    override def removeClient(uniqueId: String, ua: UserAnswers)(using HeaderCarrier): Future[Unit] = Future.unit
  }

  private def controller = {
    val repo = org.mockito.Mockito.mock(classOf[SessionRepository])
    when(repo.set(any)).thenReturn(Future.successful(true))
    new RemoveClientYesNoController(
      mcc.messagesApi,
      repo,
      new FakeAgentIdentifierAction(bodyParsers),
      new PassThroughStatusGuard(new GamblingService(null)),
      new ClientListCheckNavigator(),
      new FakeDataRetrievalAction(Some(ua)),
      new DataRequiredActionImpl(),
      new PassThroughHasClientGuard(new GamblingService(null), null, null),
      new RemoveClientYesNoFormProvider(),
      new StubManageService(),
      mcc,
      view
    )
  }

  "onPageLoad" - {
    "renders the confirmation page for a known client" in {
      val result = controller.onPageLoad("u1")(addCSRFToken(FakeRequest()))
      status(result) mustBe OK
      contentAsString(result) must include("Acme Casinos")
    }

    "redirects to JourneyRecovery for an unknown client" in {
      val result = controller.onPageLoad("missing")(addCSRFToken(FakeRequest()))
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }

  "onSubmit" - {
    "removes the client and redirects to client-removed when Yes" in {
      val request = FakeRequest().withFormUrlEncodedBody("value" -> "true")
      val result  = controller.onSubmit("u1")(request)
      redirectLocation(result).value mustBe routes.ClientRemovedController.onPageLoad().url
    }

    "redirects back to manage-client-details when No" in {
      val request = FakeRequest().withFormUrlEncodedBody("value" -> "false")
      val result  = controller.onSubmit("u1")(request)
      redirectLocation(result).value mustBe routes.ManageClientDetailsController.onPageLoad().url
    }

    "returns BadRequest when nothing is selected" in {
      val request = addCSRFToken(FakeRequest().withFormUrlEncodedBody())
      val result  = controller.onSubmit("u1")(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}
