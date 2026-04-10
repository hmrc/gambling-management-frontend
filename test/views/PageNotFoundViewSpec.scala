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

package views

import base.SpecBase
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.PageNotFoundView

class PageNotFoundViewSpec extends SpecBase {

  "PageNotFoundView" - {

    "must render the page with correct heading and service desk link" in new Setup {

      val serviceDeskUrl =
        "https://www.gov.uk/find-hmrc-contacts/technical-support-with-hmrc-online-services"

      val html = view(serviceDeskUrl)
      val doc  = Jsoup.parse(html.body)

      doc.title must include(messages("pageNotFound.title"))

      doc.select("h1").text must include(messages("pageNotFound.heading"))

      doc.select("p").text must include(messages("pageNotFound.p1"))
      doc.select("p").text must include(messages("pageNotFound.p2"))

      val link = doc.select("#service-desk-link")

      link.attr("href") mustEqual serviceDeskUrl
      link.attr("target") mustEqual "_blank"
      link.attr("rel") must include("noreferrer")
    }
  }

  trait Setup {
    val app  = applicationBuilder().build()
    val view = app.injector.instanceOf[PageNotFoundView]

    implicit val request: play.api.mvc.Request[?] = FakeRequest()

    implicit val messages: Messages =
      app.injector
        .instanceOf[play.api.i18n.MessagesApi]
        .preferred(request)
  }
}
