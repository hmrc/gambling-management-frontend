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
import forms.clientdetails.{CreateClientReferenceFormProvider, RemoveClientFormProvider}
import org.jsoup.Jsoup
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import viewmodels.govuk.summarylist.{ActionItemViewModel, FluentActionItem}
import views.clientdetails.ManageClientDetailsViewUtil.*
import views.html.clientdetails.*

class ManageClientDetailsViewSpec extends SpecBase {

  private val app                          = applicationBuilder().build()
  private implicit val request: Request[?] =
    play.api.test.CSRFTokenHelper.addCSRFToken(FakeRequest())
  private implicit val messages: Messages  = app.injector.instanceOf[MessagesApi].preferred(request)

  "ManageClientDetailsView" - {

    "renders the headers and title correctly" in {

      val view = app.injector.instanceOf[ManageClientDetailsView]
      val doc  = Jsoup.parse(view("XMM00000000123", summaryListWithChangeAndRemove(messages)).body)

      doc.text must include("Manage your client details")
      doc.text must include("Client name")
      doc.text must include("MGD registration number")
      doc.text must include("Client reference")
      doc.text must include("Remove client")
    }

    "renders the client details and change/remove links when clientReference is populated" in {

      val view = app.injector.instanceOf[ManageClientDetailsView]
      val doc  = Jsoup.parse(view("XMM00000000123", summaryListWithChangeAndRemove(messages)).body)

      doc.text must include("Acme Casinos")
      doc.text must include("XMM00000000123")
      doc.text must include("clientRef123")

      val ddActions = doc.select("dd").toArray
      ddActions(3).toString must include("<dd class=\"govuk-summary-list__actions govuk-!-width-one-third\">")

      val ddHrefs = doc.select("dd>ul a").eachAttr("href").toArray
      ddHrefs must contain(
        controllers.clientdetails.routes.CreateClientReferenceController.onPageLoad("XMM00000000123").url
      )
      // ddHrefs must contain(controllers.clientdetails.routes.RemoveClientReferenceController.onPageLoad("XMM00000000123").url) TODO : uncomment
      ddHrefs must have size 2

      val pHrefs = doc.select("p.govuk-body a").eachAttr("href").toArray
      pHrefs must contain(
        controllers.clientdetails.routes.RemoveClientController.onPageLoad("XMM00000000123").url
      )
    }

    "renders the client details and change link ONLY when clientReference is EMPTY" in {

      val view = app.injector.instanceOf[ManageClientDetailsView]
      val doc  = Jsoup.parse(view("XMM00000000123", summaryListWithChangeOnly(messages)).body)

      doc.text must include("Acme Casinos")
      doc.text must include("XMM00000000123")
      doc.text must include("Not provided")

      val ddActions = doc.select("dd").toArray
      ddActions(3).toString must include("<dd class=\"govuk-summary-list__actions govuk-!-width-one-third\">")

      val ddHrefs = doc.select("dd a").eachAttr("href").toArray
      ddHrefs must contain(
        controllers.clientdetails.routes.CreateClientReferenceController.onPageLoad("XMM00000000123").url
      )
      ddHrefs must have size 1

      val pHrefs = doc.select("p.govuk-body a").eachAttr("href").toArray
      pHrefs must contain(
        controllers.clientdetails.routes.RemoveClientController.onPageLoad("XMM00000000123").url
      )
    }
  }
}

object ManageClientDetailsViewUtil {
  def summaryListWithChangeAndRemove(messages: Messages) = SummaryList(
    rows = Seq(
      SummaryListRow(
        key = Key(Text(messages("agent.manageClientDetails.clientName"))),
        value = Value(Text("Acme Casinos"))
      ),
      SummaryListRow(
        key = Key(Text(messages("agent.manageClientDetails.regNumber"))),
        value = Value(Text("XMM00000000123"))
      ),
      SummaryListRow(
        key = Key(Text(messages("agent.manageClientDetails.clientReference"))),
        value = Value(Text("clientRef123")),
        actions = Some(
          Actions(
            classes = "govuk-!-width-one-third",
            items = Seq(
              ActionItemViewModel(
                Text("ChangeLink"),
                "/manage-gambling-tax/agent/manage-your-details/create-client-reference/XMM00000000123"
              ).withVisuallyHiddenText(messages("")),
              ActionItemViewModel(
                Text("RemoveLink"),
                "/manage-gambling-tax/agent/remove-client-reference/XMM00000000123" // TODO: use correct url
              ).withVisuallyHiddenText(messages(""))
            )
          )
        )
      )
    )
  )

  def summaryListWithChangeOnly(messages: Messages) = SummaryList(
    rows = Seq(
      SummaryListRow(key = Key(Text("Row 1")), value = Value(Text("Acme Casinos"))),
      SummaryListRow(key = Key(Text("Row 2")), value = Value(Text("XMM00000000123"))),
      SummaryListRow(
        key = Key(Text("Row 3")),
        value = Value(Text("Not provided")),
        actions = Some(
          Actions(
            classes = "govuk-!-width-one-third",
            items = Seq(
              ActionItemViewModel(
                Text("ChangeLink"),
                "/manage-gambling-tax/agent/manage-your-details/create-client-reference/XMM00000000123"
              ).withVisuallyHiddenText(messages(""))
            )
          )
        )
      )
    )
  )

}
