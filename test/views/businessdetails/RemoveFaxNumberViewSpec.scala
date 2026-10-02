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
import forms.businessdetails.RemoveFaxNumberFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.businessdetails.RemoveFaxNumberView

import java.time.LocalDate

class RemoveFaxNumberViewSpec extends SpecBase {

  "RemoveFaxNumberView" - {

    "must render the page with the correct content" in new Setup {
      val html: HtmlFormat.Appendable = view(form, faxNumber, NormalMode)
      val doc: Document               = Jsoup.parse(html.body)

      doc.title must include(messages("removeFaxNumber.title"))
      doc.select(".govuk-caption-l").text mustBe
        messages("removeFaxNumber.caption")

      doc.select("legend").text must include(messages("removeFaxNumber.heading", faxNumber))

      doc.select("input[value=true]").isEmpty mustBe false
      doc.select("input[value=false]").isEmpty mustBe false

      doc.select("button").text mustBe messages("site.continue")
    }

    "must render an error summary when the form has errors" in new Setup {

      val boundForm = form.bind(Map("value" -> ""))
      val html      = view(boundForm, faxNumber, NormalMode)
      val doc       = Jsoup.parse(html.body)

      doc.select(".govuk-error-summary").isEmpty mustBe false
      doc.select(".govuk-error-summary__list a").text must include(
        messages("removeFaxNumber.error.required")
      )
    }

    "must select Yes when the form value is true" in new Setup {

      val boundForm = form.fill(true)
      val html      = view(boundForm, faxNumber, NormalMode)
      val doc       = Jsoup.parse(html.body)

      doc.select("input[value=true]").first().hasAttr("checked") mustBe true
    }

    "must select No when the form value is false" in new Setup {

      val boundForm = form.fill(false)
      val html      = view(boundForm, faxNumber, NormalMode)
      val doc       = Jsoup.parse(html.body)

      doc.select("input[value=false]").first().hasAttr("checked") mustBe true
    }
  }

  trait Setup {
    val app = applicationBuilder().build()

    val view      = app.injector.instanceOf[RemoveFaxNumberView]
    val form      = new RemoveFaxNumberFormProvider()()
    val faxNumber = "0999 7777 3333"

    implicit val request: play.api.mvc.Request[?] = FakeRequest()

    implicit val messages: Messages =
      app.injector
        .instanceOf[play.api.i18n.MessagesApi]
        .preferred(request)
  }
}
