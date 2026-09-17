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
import forms.clientdetails.ChangeClientReferenceFormProvider
import navigation.ClientListCheckNavigator
import play.api.mvc.PlayBodyParsers
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.GamblingService
import views.html.clientdetails.ChangeClientReferenceView

import scala.concurrent.ExecutionContext.Implicits.global

class ChangeClientReferenceControllerSpec extends SpecBase {

  private val app         = applicationBuilder().build()
  private val mcc         = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
  private val bodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val view        = app.injector.instanceOf[ChangeClientReferenceView]

  private def controller =
    new ChangeClientReferenceController(
      mcc.messagesApi,
      sessionRepository = null,
      new FakeAgentIdentifierAction(bodyParsers),
      new PassThroughStatusGuard(new GamblingService(null)),
      new ClientListCheckNavigator(),
      new FakeDataRetrievalAction(Some(emptyUserAnswers)),
      new DataRequiredActionImpl(),
      new PassThroughHasClientGuard(new GamblingService(null), null, null),
      new ChangeClientReferenceFormProvider(),
      manageService = null,
      mcc,
      view
    )

  "onPageLoad" - {
    "renders the change-reference form" in {
      val result = controller.onPageLoad("u1")(addCSRFToken(FakeRequest()))
      status(result) mustBe OK
      contentAsString(result) must include(messages(app)("changeClientReference.heading"))
    }
  }

  "onSubmit" - {
    "returns BadRequest and re-renders the form when the value is empty" in {
      val request = addCSRFToken(FakeRequest().withFormUrlEncodedBody("value" -> ""))
      val result  = controller.onSubmit("u1")(request)
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(messages(app)("changeClientReference.error.required"))
    }
  }
}
