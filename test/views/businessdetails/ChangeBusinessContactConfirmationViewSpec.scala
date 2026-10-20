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
import views.html.businessdetails.ChangeBusinessContactConfirmationView

class ChangeBusinessContactConfirmationViewSpec extends SpecBase {

  "ChangeBusinessContactConfirmationView" - {

    "must render the page with the correct title and content" in new Setup {

      val html =
        view(
          acknowledgementReference = acknowledgementReference,
          submissionTimestamp = submissionTimestamp
        )

      val doc = Jsoup.parse(html.body)

      doc.title must include(
        messages("changeBusinessContactConfirmation.title")
      )

      doc.title must include(
        messages("service.name")
      )

      doc.title must include(
        messages("site.govuk")
      )

      doc.select("h1.govuk-panel__title").text() mustEqual
        messages("changeBusinessContactConfirmation.heading")

      doc.select(".govuk-panel__body").text() must include(
        messages("changeBusinessContactConfirmation.reference")
      )

      doc.select(".govuk-panel__body strong").text() mustEqual
        acknowledgementReference

      doc.select("main h2.govuk-heading-m").text() mustEqual
        messages("changeBusinessContactConfirmation.whatHappensNext")

      doc.select("main").text() must include(
        messages("changeBusinessContactConfirmation.reviewing")
      )

      doc.select("main").text() must include(
        messages("changeBusinessContactConfirmation.processingTime")
      )
    }

    "must render the acknowledgement reference" in new Setup {

      val html =
        view(
          acknowledgementReference = acknowledgementReference,
          submissionTimestamp = submissionTimestamp
        )

      val doc = Jsoup.parse(html.body)

      val panelBody = doc.select(".govuk-panel__body")

      panelBody.text() must include(
        messages("changeBusinessContactConfirmation.reference")
      )

      panelBody.select("strong").text() mustEqual
        acknowledgementReference
    }

    "must render the what happens next heading" in new Setup {

      val html =
        view(
          acknowledgementReference = acknowledgementReference,
          submissionTimestamp = submissionTimestamp
        )

      val doc = Jsoup.parse(html.body)

      doc.select("main h2.govuk-heading-m").text() mustEqual
        messages("changeBusinessContactConfirmation.whatHappensNext")
    }

    "must render the return to business details link" in new Setup {

      val html =
        view(
          acknowledgementReference = acknowledgementReference,
          submissionTimestamp = submissionTimestamp
        )

      val doc = Jsoup.parse(html.body)

      val returnLink =
        doc
          .select(
            s"""main a[href="${controllers.businessdetails.routes.ChangeBusinessContactController
                .onPageLoad()
                .url}"]"""
          )
          .first()

      returnLink must not be null

      returnLink.text() mustEqual
        messages("changeBusinessContactConfirmation.returnLink")

      returnLink.attr("href") mustEqual
        controllers.businessdetails.routes.ChangeBusinessContactController
          .onPageLoad()
          .url
    }

    "must not display the submission timestamp when it is not required by the page design" in new Setup {

      val html =
        view(
          acknowledgementReference = acknowledgementReference,
          submissionTimestamp = submissionTimestamp
        )

      val doc = Jsoup.parse(html.body)

      doc.select("main").text() must not include submissionTimestamp
    }
  }

  trait Setup {

    val app =
      applicationBuilder().build()

    val view =
      app.injector.instanceOf[ChangeBusinessContactConfirmationView]

    val acknowledgementReference =
      "ABC123"

    val submissionTimestamp =
      "2026-10-12T12:00:00"

    implicit val request: Request[?] =
      FakeRequest()

    implicit val messages: Messages =
      app.injector
        .instanceOf[MessagesApi]
        .preferred(request)
  }
}
