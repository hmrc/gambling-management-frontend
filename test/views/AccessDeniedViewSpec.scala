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
import views.html.AccessDeniedView

class AccessDeniedViewSpec extends SpecBase {

  "AccessDeniedView" - {

    "must render the page with correct heading and service desk link" in new Setup {

      val html = view("/")
      val doc = Jsoup.parse(html.body)

      doc.title must include(messages("accessDenied.title"))

      doc.select("h1").text must include(messages("accessDenied.heading"))

      doc.select("p").text must include(messages("accessDenied.p1"))
      val link = doc.select(".page-not-found-home-page-link a").attr("href")

      link mustEqual controllers.routes.IndexController.onPageLoad().url

    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val view = app.injector.instanceOf[AccessDeniedView]

    implicit val request: play.api.mvc.Request[?] = FakeRequest()

    implicit val messages: Messages =
      app.injector
        .instanceOf[play.api.i18n.MessagesApi]
        .preferred(request)
  }

}
