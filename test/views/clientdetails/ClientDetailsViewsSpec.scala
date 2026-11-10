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

package views.clientdetails

import base.SpecBase
import forms.clientdetails.{ChangeClientReferenceFormProvider, RemoveClientFormProvider, RemoveClientReferenceFormProvider}
import org.jsoup.Jsoup
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.clientdetails.*

class ClientDetailsViewsSpec extends SpecBase {

  private val app                          = applicationBuilder().build()
  private implicit val request: Request[?] =
    play.api.test.CSRFTokenHelper.addCSRFToken(FakeRequest())
  private implicit val messages: Messages  = app.injector.instanceOf[MessagesApi].preferred(request)

  "ChangeClientReferenceView" - {
    val view = app.injector.instanceOf[ChangeClientReferenceView]
    val form = new ChangeClientReferenceFormProvider()()

    "renders the input pre-filled and posts to onSubmit" in {
      val doc = Jsoup.parse(view(form.fill("existing"), "u1").body)
      doc.select("input[name=value]").attr("value") mustBe "existing"
      doc.select("form").attr("action") mustBe
        controllers.clientdetails.routes.ChangeClientReferenceController.onSubmit("u1").url
    }

    "renders an error summary when the form has errors" in {
      val doc = Jsoup.parse(view(form.bind(Map("value" -> "")), "u1").body)
      doc.select(".govuk-error-summary").size mustBe 1
      doc.text must include(messages("changeClientReference.error.required"))
    }
  }

  "RemoveClientView" - {
    val view = app.injector.instanceOf[RemoveClientView]
    val form = new RemoveClientFormProvider()()

    "renders yes/no radios and posts to onSubmit" in {
      val doc = Jsoup.parse(view("Acme Casinos", form, "u1").body)
      doc.select("input[type=radio]").size mustBe 2
      doc.select("form").attr("action") mustBe
        controllers.clientdetails.routes.RemoveClientController.onSubmit("u1").url
      doc.text must include("Acme Casinos")
    }

    "renders an error when nothing is selected" in {
      val doc = Jsoup.parse(view("Acme Casinos", form.bind(Map.empty[String, String]), "u1").body)
      doc.select(".govuk-error-summary").size mustBe 1
    }
    "renders the caption" in {
      val doc = Jsoup.parse(
        view("Acme Casinos", form, "u1").body
      )

      doc.select(".govuk-caption-l").text() mustBe
        messages("removeClient.caption")
    }

    "renders the dynamic heading" in {
      val doc = Jsoup.parse(
        view("Acme Casinos", form, "u1").body
      )

      doc.select("h1").text() mustBe
        messages("removeClient.heading", "Acme Casinos")
    }

    "renders the body content" in {
      val doc = Jsoup.parse(
        view("Acme Casinos", form, "u1").body
      )

      doc.select(".govuk-body").text() mustBe
        messages("removeClient.body")
    }

    "renders the Continue button" in {
      val doc = Jsoup.parse(
        view("Acme Casinos", form, "u1").body
      )

      doc.select(".govuk-button").text() mustBe
        messages("site.continue")
    }
  }

  "RemoveClientReferenceView" - {

    val view =
      app.injector.instanceOf[RemoveClientReferenceView]

    val form =
      new RemoveClientReferenceFormProvider()()

    val regNumber       = "XMM00000000123"
    val clientReference = "my-client-reference"

    "renders yes/no radios and posts to onSubmit" in {

      val doc = Jsoup.parse(
        view(
          form,
          regNumber,
          clientReference
        ).body
      )

      doc.select("input[type=radio]").size mustBe 2

      doc.select("form").attr("action") mustBe
        controllers.clientdetails.routes.RemoveClientReferenceController
          .onSubmit(regNumber)
          .url
    }

    "renders the client reference in the heading" in {

      val doc = Jsoup.parse(
        view(
          form,
          regNumber,
          clientReference
        ).body
      )

      doc.select("h1").text() mustBe
        messages(
          "removeClientReference.heading",
          clientReference
        )
    }

    "renders the client reference in the page title" in {

      val doc = Jsoup.parse(
        view(
          form,
          regNumber,
          clientReference
        ).body
      )

      doc.title() must include(
        messages(
          "removeClientReference.title",
          clientReference
        )
      )
    }

    "renders the caption" in {

      val doc = Jsoup.parse(
        view(
          form,
          regNumber,
          clientReference
        ).body
      )

      doc.select(".govuk-caption-l").text() mustBe
        messages("removeClientReference.caption")
    }

    "renders the Continue button" in {

      val doc = Jsoup.parse(
        view(
          form,
          regNumber,
          clientReference
        ).body
      )

      doc.select(".govuk-button").text() mustBe
        messages("site.continue")
    }

    "renders an error when nothing is selected" in {

      val formWithErrors =
        form.bind(Map.empty[String, String])

      val doc = Jsoup.parse(
        view(
          formWithErrors,
          regNumber,
          clientReference
        ).body
      )

      doc.select(".govuk-error-summary").size mustBe 1

      doc.select(".govuk-form-group--error").size mustBe 1
    }
  }

  "ClientRefUpdateConfirmationView" - {
    "renders the confirmation panel" in {
      val doc = Jsoup.parse(app.injector.instanceOf[ClientRefUpdateConfirmationView].apply().body)
      doc.select(".govuk-panel__title").text must include(messages("clientRefUpdateConfirmation.heading"))
    }
  }

  "ClientRemovedView" - {
    "renders the removed confirmation panel" in {
      val doc = Jsoup.parse(app.injector.instanceOf[ClientRemovedView].apply().body)
      doc.select(".govuk-panel__title").text must include(messages("clientRemoved.heading"))
    }
  }
}
