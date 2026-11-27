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
import models.UserAnswers
import models.agent.AgentClient
import models.agent.AgentClientUtil.clientsList
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.clientdetails.ClientReferencePage
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.mvc.PlayBodyParsers
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{GamblingService, ManageService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientRefUpdateConfirmationControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val view        = app.injector.instanceOf[views.html.clientdetails.ClientRefUpdateConfirmationView]

  private val validRegNum    = "RegNum123"
  private val regNumNotFound = "RegNum-NOT_FOUND"
  private val client         = AgentClient("mgd", validRegNum, Some("Acme Casinos"), Some("myref"))

  private class StubManageService(repoResult: Future[Unit] = Future.unit) extends ManageService(null, null) {
    override def updateClient(regNumber: String, ua: UserAnswers, clientRef: String)(using
      HeaderCarrier
    ): Future[Unit] = repoResult
  }

  private def controller(ua: UserAnswers, repoResult: Future[Unit] = Future.unit) = {
    val repo = org.mockito.Mockito.mock(classOf[SessionRepository])
    when(repo.set(any)).thenReturn(repoResult)
    new ClientRefUpdateConfirmationController(
      mcc.messagesApi,
      mcc,
      new FakeAgentIdentifierAction(bodyParsers),
      new PassThroughClientListCheckAction(),
      new FakeDataRetrievalAction(Some(ua)),
      new DataRequiredActionImpl(),
      new PassThroughHasClientGuard(new GamblingService(null), null, null),
      new StubManageService(repoResult),
      view
    )
  }

  "onPageLoad" - {
    "renders the selected client's details" in {
      val ua               =
        UserAnswers("internal-id")
          .set(AgentClientsPage, List(client))
          .flatMap(_.set(SelectedClientPage, validRegNum))
          .get
      val populatedAnswers = ua.set(ClientReferencePage, "01234567890123456789").success.value
      val result           = controller(populatedAnswers).onPageLoad(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) must include("01234567890123456789")
      contentAsString(result) must include(
        controllers.clientdetails.routes.ClientReferenceController.onPageLoad(validRegNum).url
      )
      contentAsString(result) must include(
        controllers.clientdetails.routes.ClientRefUpdateConfirmationController.onSubmit().url
      )
    }

    "redirects to JourneyRecovery when no client is selected" in {
      val ua     = UserAnswers("internal-id").set(AgentClientsPage, List(client)).get
      val result = controller(ua).onPageLoad(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }

  "onSubmit" - {
    "updates the client reference and redirects to ManageClientDetailsController" in {
      val ua     =
        UserAnswers("internal-id")
          .set(AgentClientsPage, List(client))
          .flatMap(_.set(SelectedClientPage, validRegNum))
          .flatMap(_.set(ClientReferencePage, "client-ref-1234"))
          .get
      val result = controller(ua).onSubmit()(FakeRequest())
      redirectLocation(result).value mustBe routes.ManageClientDetailsController.onPageLoad().url
    }

    "redirects to the SystemErrorController when manageService throws error" in {
      val ua     =
        UserAnswers("internal-id")
          .set(AgentClientsPage, List(client))
          .flatMap(_.set(SelectedClientPage, validRegNum))
          .flatMap(_.set(ClientReferencePage, "client-ref-1234"))
          .get
      val result = controller(ua, Future.failed(new RuntimeException("boom"))).onSubmit()(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
    }

    "redirects to the JourneyRecoveryController when regNum is not found in the clientList" in {
      val ua     =
        UserAnswers("internal-id")
          .set(AgentClientsPage, List(client))
          .flatMap(_.set(SelectedClientPage, regNumNotFound))
          .flatMap(_.set(ClientReferencePage, "client-ref-1234"))
          .get
      val result = controller(ua, Future.failed(new RuntimeException("boom"))).onSubmit()(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }

    "redirects to the JourneyRecoveryController when ClientReference does not exist in userAnswers" in {
      val ua     =
        UserAnswers("internal-id")
          .set(AgentClientsPage, List(client))
          .flatMap(_.set(SelectedClientPage, validRegNum))
          .get
      val result = controller(ua, Future.failed(new RuntimeException("boom"))).onSubmit()(FakeRequest())
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }
}
