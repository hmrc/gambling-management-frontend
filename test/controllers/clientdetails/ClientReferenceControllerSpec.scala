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
import forms.clientdetails.ClientReferenceFormProvider
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.clientdetails.ClientReferencePage
import play.api.mvc.PlayBodyParsers
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{GamblingService, ManageService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.clientdetails.ClientReferenceView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class ClientReferenceControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val view        = app.injector.instanceOf[ClientReferenceView]

  private class StubManageService extends ManageService(null, null) {
    override def updateClient(regNumber: String, ua: UserAnswers, clientRef: String)(using
      HeaderCarrier
    ): Future[Unit] =
      Future.unit
  }

  private def controller(
    repoResult: Future[Any] = Future.successful(true),
    userAnswers: UserAnswers = emptyUserAnswers
  ) = {
    val repo = org.mockito.Mockito.mock(classOf[SessionRepository])
    when(repo.set(any)).thenReturn(repoResult)
    new ClientReferenceController(
      mcc.messagesApi,
      repo,
      new FakeAgentIdentifierAction(bodyParsers),
      new PassThroughClientListCheckAction(),
      new FakeDataRetrievalAction(Some(userAnswers)),
      new DataRequiredActionImpl(),
      new PassThroughHasClientGuard(new GamblingService(null), null, null),
      new ClientReferenceFormProvider(),
      new StubManageService(),
      mcc,
      view
    )
  }

  "onPageLoad" - {
    "renders the change-reference form" in {
      val result = controller().onPageLoad("RegNum123")(addCSRFToken(FakeRequest()))
      status(result) mustBe OK
      contentAsString(result) must include(messages(app)("agent.clientReference.heading"))
    }

    "renders the change-reference form with the existing user-answers value" in {
      val populatedAnswers = emptyUserAnswers.set(ClientReferencePage, "012345678901234567891").success.value
      val result           = controller(userAnswers = populatedAnswers).onPageLoad("RegNum123")(addCSRFToken(FakeRequest()))
      status(result) mustBe OK
      contentAsString(result) must include(messages(app)("agent.clientReference.heading"))
      contentAsString(result) must include("01234567890123456789")
    }
  }

  "onSubmit" - {
    "updates the client reference and redirects to the confirmation page" in {
      val request = FakeRequest().withFormUrlEncodedBody("value" -> "newref")
      val result  = controller().onSubmit("RegNum123")(request)
      redirectLocation(result).value mustBe routes.ClientRefUpdateConfirmationController.onPageLoad().url
    }

    "returns BadRequest and re-renders the form when the value is empty" in {
      val request = addCSRFToken(FakeRequest().withFormUrlEncodedBody("value" -> ""))
      val result  = controller().onSubmit("RegNum123")(request)
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(messages(app)("agent.clientReference.error.required"))
    }

    "returns BadRequest and re-renders the form when the value is more than 21 characters" in {
      val request = addCSRFToken(FakeRequest().withFormUrlEncodedBody("value" -> "abcdefghijklmnopqrstuV"))
      val result  = controller().onSubmit("RegNum123")(request)
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(messages(app)("agent.clientReference.error.length"))
    }

    "returns BadRequest and re-renders the form when the value does not match the RegEx" in {
      val request = addCSRFToken(FakeRequest().withFormUrlEncodedBody("value" -> "abcdefghijklmnopqrstu%%%%"))
      val result  = controller().onSubmit("RegNum123")(request)
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(messages(app)("agent.clientReference.error.invalid"))
    }

    "redirects to the SystemErrorController when sessionRepository throws error" in {
      val request = FakeRequest().withFormUrlEncodedBody("value" -> "newref")
      val result  = controller(Future.failed(new RuntimeException("boom"))).onSubmit("RegNum123")(request)
      redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
    }
  }
}
