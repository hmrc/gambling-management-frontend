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

package views.agent

import base.SpecBase
import org.jsoup.Jsoup
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.agent.NoResultsFoundView

class NoResultsFoundViewSpec extends SpecBase {

  private val app                          = applicationBuilder().build()
  private implicit val request: Request[?] = FakeRequest()
  private implicit val messages: Messages  = app.injector.instanceOf[MessagesApi].preferred(request)
  private val view                         = app.injector.instanceOf[NoResultsFoundView]

  "NoResultsFoundView" - {
    "renders the heading, playback of the search term, suggestions and search again link" in {
      val doc = Jsoup.parse(view("Acme").body)

      doc.select("h1").text must include(messages("noResultsFound.heading"))
      doc.select(".govuk-caption-l").text mustBe messages("noResultsFound.caption")
      doc.text              must include(messages("noResultsFound.p1", "Acme"))
      doc.select("ul.govuk-list--bullet li").size mustBe 3
      doc.select(".govuk-back-link").size mustBe 1
      doc.select("a:contains(" + messages("noResultsFound.searchAgain") + ")").attr("href") mustBe
        controllers.agent.routes.ClientListController.onPageLoad().url
    }

    "escapes the search term" in {
      val doc = Jsoup.parse(view("<script>alert(1)</script>").body)

      doc.select("main script").size mustBe 0
      doc.text must include("<script>alert(1)</script>")
    }
  }
}
