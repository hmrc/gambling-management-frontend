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
import forms.clientdetails.{ChangeClientReferenceFormProvider, RemoveClientFormProvider}
import org.jsoup.Jsoup
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import viewmodels.govuk.summarylist.{ActionItemViewModel, FluentActionItem}
import views.html.clientdetails.*

class ManageClientDetailsViewSpec extends SpecBase {

  private val app                          = applicationBuilder().build()
  private implicit val request: Request[?] =
    play.api.test.CSRFTokenHelper.addCSRFToken(FakeRequest())
  private implicit val messages: Messages  = app.injector.instanceOf[MessagesApi].preferred(request)

  "ManageClientDetailsView" - {
    "renders the client details and change/remove links" in {
      val summaryList = SummaryList(
        rows = Seq(
          SummaryListRow(key = Key(Text("Row 1")), value = Value(Text("Acme Casinos"))),
          SummaryListRow(key = Key(Text("Row 2")), value = Value(Text("XMM00000000123"))),
          SummaryListRow(
            key = Key(Text("Row 3")),
            value = Value(Text("clientRef123")),
            actions = Some(
              Actions(
                classes = "govuk-!-width-one-third",
                items = Seq(
                  ActionItemViewModel(
                    Text("ChangeLink"),
                    "/manage-gambling-tax/agent/change-client-reference/XMM00000000123"
                  ).withVisuallyHiddenText(messages("")),
                  ActionItemViewModel(
                    Text("RemoveLink"),
                    "/manage-gambling-tax/agent/manage-your-details/remove-client/XMM00000000123" //TODO: use correct url
                  ).withVisuallyHiddenText(messages(""))
                )
              )
            )
          )
        )
      )

      val view = app.injector.instanceOf[ManageClientDetailsView]
      val doc  = Jsoup.parse(view("Acme Casinos", summaryList).body)

      doc.text must include("Acme Casinos")
      doc.text must include("XMM00000000123")
      doc.text must include("clientRef123")

      val hrefs = doc.select("a").eachAttr("href").toArray
      hrefs must contain(
        controllers.clientdetails.routes.ChangeClientReferenceController.onPageLoad("XMM00000000123").url
      )
      hrefs must contain(controllers.clientdetails.routes.RemoveClientController.onPageLoad("XMM00000000123").url)
    }
  }
}
