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
import play.api.test.FakeRequest
import views.html.agent.ManageYourDetailsView

class ManageYourDetailsViewSpec extends SpecBase {

  "ManageYourDetailsView" - {
    "must render heading, agent name, both cards and the business details link" in {
      val app           = applicationBuilder().build()
      val view          = app.injector.instanceOf[ManageYourDetailsView]
      implicit val msgs = messages(app)
      val doc           = Jsoup.parse(view("Agent 1", isClientListEmpty = false)(FakeRequest(), msgs).body)

      doc.title() must include("Manage your details")
      doc.select("h1").text() mustBe "Manage your details"
      doc.select("main p.govuk-body").first().text() mustBe "Agent 1"
      doc.select(".card-heading").eachText().toArray mustBe Array("Client search", "Your business details")
      doc.select(".card-action a").size() mustBe 3
      doc
        .select(".card-action a[href=\"" + controllers.agent.routes.ClientListController.onPageLoad().url + "\"]")
        .size() mustBe 1
      doc
        .select(
          ".card-action a[href=\"" + controllers.agent.routes.ClientListController.downloadClientList().url + "\"]"
        )
        .size() mustBe 1
      doc.select(".card-action a").last().attr("href") mustBe
        controllers.businessdetails.routes.CheckBusinessDetailsController.onPageLoad().url
    }

    "must show the empty message instead of client links when there are no clients" in {
      val app           = applicationBuilder().build()
      val view          = app.injector.instanceOf[ManageYourDetailsView]
      implicit val msgs = messages(app)
      val doc           = Jsoup.parse(view("Agent 1", isClientListEmpty = true)(FakeRequest(), msgs).body)

      doc.select(".card-action").first().text() mustBe "You have no clients to view."
      doc.select(".card-action a").size() mustBe 1
    }
  }
}
