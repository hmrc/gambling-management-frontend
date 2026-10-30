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
import controllers.actions.{DataRequiredActionImpl, FakeAgentIdentifierAction, FakeDataRetrievalAction, PassThroughClientListCheckAction}
import controllers.routes
import models.UserAnswers
import models.agent.*
import models.agent.AgentClientUtil.clientsList
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{GamblingService, ManageService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.agent.ClientListView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientListControllerSpec extends SpecBase {

  private val app             = applicationBuilder().build()
  private val mcc             = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
  private val bodyParsers     = app.injector.instanceOf[PlayBodyParsers]
  private val view            = app.injector.instanceOf[ClientListView]
  private val clientListCheck = new PassThroughClientListCheckAction()

  private class StubManageService(clients: List[AgentClient]) extends ManageService(null, null) {
    override def resolveAndStoreAgentClients(ua: UserAnswers)(using
      HeaderCarrier
    ): Future[(List[AgentClient], UserAnswers)] =
      Future.successful((clients, ua))
  }

  private def controller(clients: List[AgentClient], userAnswers: Option[UserAnswers]) =
    new ClientListController(
      mcc.messagesApi,
      new FakeAgentIdentifierAction(bodyParsers),
      clientListCheck,
      new FakeDataRetrievalAction(userAnswers),
      new DataRequiredActionImpl(),
      new StubManageService(clients),
      mcc,
      view
    )

  "onPageLoad" - {

    "renders the client list when clients are returned" in {
      val result = controller(clientsList, Some(emptyUserAnswers)).onPageLoad()(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) must include("ABC Ltd")
    }

    "redirects to no-authorised-clients when the list is Nil" in {
      val result = controller(Nil, Some(emptyUserAnswers)).onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.agent.routes.NoAuthorisedClientsController.onPageLoad().url
    }
  }

  "downloadClientList" - {

    "must return a CSV file with all mock clients" in {

      val result = controller(clientsList, Some(emptyUserAnswers)).downloadClientList()(FakeRequest())

      status(result) mustBe OK
      contentType(result) mustBe Some("text/csv")
      header("Content-Disposition", result).value mustBe "attachment; filename=MgdAgentClientList.csv"

      val expectedBody = Seq(
        expectedHeader,
        "\"ABC Ltd\",\"XVM00000001001\",\"EZ00100\"",
        "\"XYZ Builders\",\"XAM00000001002\",\"CD002\"",
        "\"Betelgeuse Construction 3\",\"XCM00000001003\",\"UP374\"",
        "\"Heritage Contractors 4\",\"XEM00000001004\",\"WY080\""
      ).mkString("\n")

      contentAsString(result) mustBe expectedBody
    }

    "journey recovery when no userAnswers" in {

      val result = controller(clientsList, None).downloadClientList()(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
    }

    "must neutralise CSV injection for dangerous leading characters" in {
      val dangerousClients = List(
        AgentClient(
          regime = "MGD",
          regNumber = "RN1",
          clientName = Some("""=Company("x")"""),
          agentOwnRef = Some("=ABC")
        ),
        AgentClient(
          regime = "MGD",
          regNumber = "RN2",
          clientName = Some("\t=Company(\"x\")"),
          agentOwnRef = Some("\t=ABC2")
        ),
        AgentClient(
          regime = "MGD",
          regNumber = "RN3",
          clientName = Some("\r=Company(\"x\")"),
          agentOwnRef = Some("\r=ABC3")
        ),
        AgentClient(
          regime = "MGD",
          regNumber = "RN4",
          clientName = Some("\n=Company(\"x\")"),
          agentOwnRef = Some("\n=ABC4")
        )
      )

      val result = controller(dangerousClients, Some(emptyUserAnswers)).downloadClientList()(FakeRequest())
      status(result) mustBe OK

      val expectedBody = Seq(
        expectedHeader,
        "\"'=Company(\"\"x\"\")\",\"RN1\",\"'=ABC\"",
        "\"'\t=Company(\"\"x\"\")\",\"RN2\",\"'\t=ABC2\"",
        "\"'\r=Company(\"\"x\"\")\",\"RN3\",\"'\r=ABC3\"",
        "\"'\n=Company(\"\"x\"\")\",\"RN4\",\"'\n=ABC4\""
      ).mkString("\n")

      contentAsString(result) mustBe expectedBody
    }

    "redirects to no-authorised-clients when the list is Nil" in {
      val result = controller(Nil, Some(emptyUserAnswers)).downloadClientList()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.agent.routes.NoAuthorisedClientsController.onPageLoad().url
    }
  }

  private def expectedHeader =
    val bom  = "\uFEFF"
    val msgs = messages(app)
    bom + Seq(
      msgs("clientList.download.th.clientName"),
      msgs("clientList.download.th.regNumber"),
      msgs("clientList.download.th.clientReference")
    ).mkString(",")
}
