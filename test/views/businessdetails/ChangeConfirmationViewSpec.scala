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
import views.html.businessdetails.ChangeConfirmationView

class ChangeConfirmationViewSpec extends SpecBase {

  "ChangeConfirmationView" - {

    "must render the panel, next steps and return link" in {
      val app                          = applicationBuilder().build()
      val view                         = app.injector.instanceOf[ChangeConfirmationView]
      implicit val request: Request[?] = FakeRequest()
      implicit val messages: Messages  = app.injector.instanceOf[MessagesApi].preferred(request)

      val doc = Jsoup.parse(view("ABC123", "/return").body)

      doc.title                 must include(messages("changeConfirmation.title"))
      doc.select("h1.govuk-panel__title").text() mustEqual messages("changeConfirmation.heading")
      doc.select(".govuk-panel__body strong").text() mustEqual "ABC1 23"
      doc.select("main h2.govuk-heading-m").text() mustEqual messages("changeConfirmation.whatHappensNext")
      doc.select("main").text() must include(messages("changeConfirmation.reviewing"))
      doc.select("main").text() must include(messages("changeConfirmation.processingTime"))
      doc.select("main a[href=/return]").text() mustEqual messages("changeConfirmation.returnLink")
    }
  }
}
