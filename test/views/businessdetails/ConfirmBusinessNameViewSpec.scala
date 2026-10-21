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
import play.api.test.FakeRequest
import views.html.businessdetails.ConfirmBusinessNameView

class ConfirmBusinessNameViewSpec extends SpecBase {

  "ConfirmBusinessNameView" - {
    "must render the page with the correct content" in new Setup {
      val html = view(businessName, changeUrl)
      val doc  = Jsoup.parse(html.body)

      doc.title     must include(messages("confirmBusinessName.title"))
      doc.select("h1").text() mustEqual messages("confirmBusinessName.heading")
      doc.body.text must include(messages("confirmBusinessName.caption"))
    }

    "must display the proposed business name" in new Setup {
      val html = view(businessName, changeUrl)
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-summary-list__key").text() mustEqual messages("confirmBusinessName.businessName")
      doc.select(".govuk-summary-list__value").text() mustEqual businessName
    }

    "must render a submit button" in new Setup {
      val html = view(businessName, changeUrl)
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-button").text() mustEqual messages("confirmBusinessName.submit")
    }

    "must render a Change link with the correct URL" in new Setup {
      val html = view(businessName, changeUrl)
      val doc  = Jsoup.parse(html.body)

      val changeLink = doc.select(".govuk-summary-list__actions a")

      changeLink.text() must include(messages("site.change"))
      changeLink.attr("href") mustEqual changeUrl
    }
  }

  trait Setup {
    val app          = applicationBuilder().build()
    val view         = app.injector.instanceOf[ConfirmBusinessNameView]
    val businessName = "Test Ltd"
    val changeUrl    = controllers.businessdetails.routes.BusinessNameController.onPageLoad().url

    implicit val request: Request[?] =
      FakeRequest()
    implicit val messages: Messages  =
      app.injector.instanceOf[MessagesApi].preferred(request)
  }
}
