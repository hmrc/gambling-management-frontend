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
import forms.clientdetails.ClientReferenceFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, Key, SummaryList, SummaryListRow, Value}
import viewmodels.govuk.summarylist.{ActionItemViewModel, FluentActionItem}
import views.html.clientdetails.ClientRefUpdateConfirmationView

class ClientRefUpdateConfirmationViewSpec extends SpecBase {

  "ClientRefUpdateConfirmationView" - {
    "must render the page with the correct content" in new Setup {
      val html = view("regNum123", summaryList)
      val doc  = Jsoup.parse(html.body)

      doc.title     must include(messages("agent.clientRefUpdateConfirmation.title"))
      doc.title     must include(messages("service.name"))
      doc.title     must include(messages("site.govuk"))
      doc.body.text must include(messages("agent.clientRefUpdateConfirmation.heading"))
      doc.body.text must include(messages("agent.clientRefUpdateConfirmation.caption"))
    }

    "must render the summary list" in new Setup {
      val html = view("regNum123", summaryList)
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-summary-list").size() mustEqual 1
      doc.select(".govuk-summary-list__row").size() mustEqual 1

      doc.select(".govuk-summary-list__row").text() must include(
        messages("agent.clientRefUpdateConfirmation.clientReference")
      )
      doc.select(".govuk-summary-list__row").text() must include(messages("site.change"))

      val ddActions = doc.select("dd").toArray
      ddActions(1).toString must include("<dd class=\"govuk-summary-list__actions govuk-!-width-one-third\">")

      val ddHrefs = doc.select("dd a").eachAttr("href").toArray
      ddHrefs must contain(
        controllers.clientdetails.routes.ClientReferenceController.onPageLoad("regNum123").url
      )
      ddHrefs must have size 1
    }

    "must render a submit button" in new Setup {
      val html = view("regNum123", summaryList)
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-button").text() mustEqual messages("site.submit")
    }
  }

  trait Setup {
    val app: Application                      = applicationBuilder().build()
    val view: ClientRefUpdateConfirmationView = app.injector.instanceOf[ClientRefUpdateConfirmationView]

    implicit val request: Request[?] =
      FakeRequest()
    implicit val messages: Messages  =
      app.injector
        .instanceOf[MessagesApi]
        .preferred(request)

    val summaryList = SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(Text(messages("agent.clientRefUpdateConfirmation.clientReference"))),
          value = Value(Text("Value 1")),
          actions = Some(
            Actions(
              classes = "govuk-!-width-one-third",
              items = Seq(
                ActionItemViewModel(
                  Text("ChangeLink"),
                  "/manage-gambling-tax/agent/manage-your-details/create-client-reference/regNum123"
                ).withVisuallyHiddenText(messages(""))
              )
            )
          )
        )
      )
    )
  }
}
