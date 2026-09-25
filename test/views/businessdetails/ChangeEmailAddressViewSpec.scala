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
import org.jsoup.Jsoup
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import models.NormalMode
import play.api.test.FakeRequest

import forms.businessdetails.ChangeEmailAddressFormProvider
import views.html.businessdetails.ChangeEmailAddressView

class ChangeEmailAddressViewSpec extends SpecBase {

  "ChangeEmailAddressView" - {
    "must render the page with the correct content" in new Setup {

      val html = view(form, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.title     must include(messages("changeEmailAddress.title"))
      doc.body.text must include(messages("changeEmailAddress.heading"))

      doc.body.text must include(messages("changeEmailAddress.caption"))
    }

    "must render a continue button" in new Setup {

      val html = view(form, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-button").text() mustEqual
        messages("site.continue")
    }

    "must render the email input field" in new Setup {

      val html = view(form, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.select("#value").size() mustEqual 1
    }

    "must render errors when the form contains errors" in new Setup {

      val boundForm =
        form.bind(Map("value" -> ""))

      val html = view(boundForm, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-error-summary").size() mustEqual 1

      doc.select(".govuk-error-message").text() must not be empty
    }
  }

  trait Setup {

    val app  = applicationBuilder().build()
    val view = app.injector.instanceOf[ChangeEmailAddressView]

    val form = new ChangeEmailAddressFormProvider()()

    implicit val request: Request[?] =
      FakeRequest()
    implicit val messages: Messages  =
      app.injector
        .instanceOf[MessagesApi]
        .preferred(request)
  }
}
