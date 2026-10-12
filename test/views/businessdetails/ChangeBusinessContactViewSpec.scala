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
import models.NormalMode
import org.jsoup.Jsoup
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryList, SummaryListRow, Value}
import views.html.businessdetails.ChangeBusinessContactView

class ChangeBusinessContactViewSpec extends SpecBase {

  "ChangeBusinessContactView" - {

    "must render the page with the correct content" in new Setup {

      val html = view(summaryList, true, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.title     must include(messages("changeBusinessContact.title"))
      doc.body.text must include(messages("changeBusinessContact.heading"))
      doc.body.text must include(messages("changeBusinessContact.caption"))
    }

    "must render the summary list" in new Setup {

      val html = view(summaryList, true, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-summary-list").size() mustEqual 1
      doc.select(".govuk-summary-list__row").size() mustEqual 3
    }

    "must render the guidance when the user has made changes" in new Setup {

      val html = view(summaryList, true, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.body.text must include(messages("changeBusinessContact.guidance"))
    }

    "must not render the guidance when the user has not made changes" in new Setup {

      val html = view(summaryList, false, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.body.text must not include messages("changeBusinessContact.guidance")
    }

    "must render a submit button" in new Setup {

      val html = view(summaryList, true, NormalMode)

      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-button").text() mustEqual
        messages("site.submit")
    }

  }

  trait Setup {

    val app  = applicationBuilder().build()
    val view = app.injector.instanceOf[ChangeBusinessContactView]

    val summaryList = SummaryList(
      rows = Seq(
        SummaryListRow(key = Key(Text("Row 1")), value = Value(Text("Value 1"))),
        SummaryListRow(key = Key(Text("Row 2")), value = Value(Text("Value 2"))),
        SummaryListRow(key = Key(Text("Row 3")), value = Value(Text("Value 3")))
      )
    )

    implicit val request: Request[?] =
      FakeRequest()

    implicit val messages: Messages =
      app.injector
        .instanceOf[MessagesApi]
        .preferred(request)
  }
}
