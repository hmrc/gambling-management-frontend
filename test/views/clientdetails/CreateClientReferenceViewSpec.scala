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
import forms.clientdetails.CreateClientReferenceFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.clientdetails.CreateClientReferenceView

class CreateClientReferenceViewSpec extends SpecBase {

  "CreateClientReferenceView" - {
    "must render the page with the correct content" in new Setup {

      val html = view(form, "regNum123")
      val doc  = Jsoup.parse(html.body)

      doc.title     must include(messages("agent.createClientReference.title"))
      doc.body.text must include(messages("agent.createClientReference.heading"))
      doc.body.text must include(messages("agent.createClientReference.caption"))
      doc.body.text must include(messages("agent.createClientReference.guidance1"))
      doc.body.text must include(messages("agent.createClientReference.guidance2"))
      doc.body.text must include(messages("agent.createClientReference.clientReference.label"))
    }

    "must render a continue button" in new Setup {

      val html = view(form, "regNum123")
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-button").text() mustEqual messages("site.continue")
    }

    "must render the clientReference input field" in new Setup {

      val html = view(form, "regNum123")
      val doc  = Jsoup.parse(html.body)

      doc.select("#value").size() mustEqual 1
    }

    "must render errors when the form contains the error for empty value" in new Setup {

      val boundForm = form.bind(Map("value" -> ""))
      val html      = view(boundForm, "regNum123")
      val doc       = Jsoup.parse(html.body)

      doc.select(".govuk-error-summary").size() mustEqual 1
      doc.select(".govuk-error-message").text() must not be empty
      doc.body.text                             must include(messages("agent.createClientReference.error.required"))
    }

    "must render errors when the form contains the error for invalid regex" in new Setup {
      val boundForm = form.bind(Map("value" -> "£££"))
      val html      = view(boundForm, "regNum123")
      val doc       = Jsoup.parse(html.body)

      doc.select(".govuk-error-summary").size() mustEqual 1
      doc.select(".govuk-error-message").text() must not be empty
      doc.body.text                             must include(messages("agent.createClientReference.error.invalid"))
    }

    "must render errors when the form contains the error for value length too long" in new Setup {
      val boundForm = form.bind(Map("value" -> "012345678901234567891ERR"))
      val html      = view(boundForm, "regNum123")
      val doc       = Jsoup.parse(html.body)

      doc.select(".govuk-error-summary").size() mustEqual 1
      doc.select(".govuk-error-message").text() must not be empty
      doc.body.text                             must include(messages("agent.createClientReference.error.length"))
    }
  }

  trait Setup {
    val app: Application                = applicationBuilder().build()
    val view: CreateClientReferenceView = app.injector.instanceOf[CreateClientReferenceView]
    val form                            = new CreateClientReferenceFormProvider()()

    implicit val request: Request[?] =
      FakeRequest()
    implicit val messages: Messages  =
      app.injector
        .instanceOf[MessagesApi]
        .preferred(request)
  }
}
