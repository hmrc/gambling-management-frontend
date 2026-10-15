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

package views.businessdetails

import base.SpecBase
import forms.businessdetails.ChangeContactNumbersFormProvider
import models.NormalMode
import models.agent.ContactNumbers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.businessdetails.ChangeContactNumbersView

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ChangeContactNumbersViewSpec extends SpecBase with Matchers with BeforeAndAfterAll {

  val app: Application             = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  implicit val request: Request[?] = FakeRequest()
  implicit val msgs: Messages      = messages(app)

  val view: ChangeContactNumbersView = app.injector.instanceOf[ChangeContactNumbersView]

  override def afterAll(): Unit = {
    Await.result(app.stop(), 10.seconds)
    super.afterAll()
  }

  val formProvider               = new ChangeContactNumbersFormProvider()
  val form: Form[ContactNumbers] = formProvider()

  "ChangeContactNumbersView" - {

    "must render the correct heading" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.heading"))
    }

    "must render the correct page title" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.title"))
    }

    "must render the 'Check business details' caption" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.caption"))
      html must include("govuk-caption-l")
    }

    "must render the paragraph text" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.legend"))
    }

    "must render the phone number and mobile number labels" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.label.phoneNumber"))
      html must include(msgs("changeContactNumbers.label.mobileNumber"))
    }

    "must render an input field with type tel autocomplete and inputmode" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include("""autocomplete="tel"""")
      html must include("""inputmode="tel"""")
    }

    "must render the Continue button" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(msgs("site.continue"))
    }

    "must render the required error plus per number field error messages when both numbers are blank" in {
      val bound     = form.bind(Map("phoneNumber" -> "", "mobileNumber" -> ""))
      val boundForm = bound.copy(errors =
        Seq(
          FormError("phoneNumber", "changeContactNumbers.error.phoneRequired")
        )
      )

      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.error.phoneRequired"))
    }

    "must render the length error when the phone number exceeds 20 characters" in {
      val boundForm    = form.bind(Map("phoneNumber" -> "012345678901234567890"))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.error.length.phoneNumber"))
    }

    "must render the invalid error when the phone number contains invalid characters" in {
      val boundForm    = form.bind(Map("phoneNumber" -> "020-7946-0958"))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.error.invalid.phoneNumber"))
    }

    "must render the length error when the mobile number exceeds 20 characters" in {
      val boundForm    = form.bind(Map("mobileNumber" -> "012345678901234567890"))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.error.length.mobileNumber"))
    }

    "must render the invalid error when the mobile number contains invalid characters" in {
      val boundForm    = form.bind(Map("mobileNumber" -> "070-0000-0000"))
      val html: String = view(boundForm, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.error.invalid.mobileNumber"))
    }

    "must post to the PhoneNumber submit URL" in {
      val html: String = view(form, NormalMode)(request, msgs).toString

      html must include(
        s"""action="${controllers.businessdetails.routes.ChangeContactNumbersController.onSubmit().url}""""
      )
    }

    "must render the same content via the render method" in {
      val html: String = view.render(form, NormalMode, request, msgs).toString

      html must include(msgs("changeContactNumbers.heading"))
    }

    "must render the same content via the f method" in {
      val html: String = view.f(form, NormalMode)(request, msgs).toString

      html must include(msgs("changeContactNumbers.heading"))
    }

    "must return itself via the ref method" in {
      view.ref mustBe view
    }
  }
}
