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
import play.api.test.FakeRequest
import views.html.ManageMachineGamesDutyView

class ManageMachineGamesDutyViewSpec extends SpecBase {

  "ManageMachineGamesDutyView (agent)" - {
    "must render the heading, agent and client details and all four cards" in {
      val app           = applicationBuilder().build()
      val view          = app.injector.instanceOf[ManageMachineGamesDutyView]
      implicit val msgs = messages(app)
      val doc           = Jsoup.parse(
        view(
          true,
          "Agent 1",
          "Client Business",
          "XWM00000001770",
          4,
          "27 January 2025",
          BigDecimal("700.19"),
          "debit"
        )(
          FakeRequest(),
          msgs
        ).body
      )

      doc.title() must include("Manage your Machine Games Duty")
      doc.select("h1").text() mustBe "Manage your Machine Games Duty"
      doc.select(".govuk-grid-column-full p.govuk-body").eachText().toArray mustBe Array(
        "Agent 1 acting on behalf of Client Business",
        "MGD registration number: XWM00000001770"
      )
      doc.select(".card-heading").eachText().toArray mustBe
        Array("Statement", "Returns", "Registration", "Client details")
      doc.select(".card-body p").eachText().toArray mustBe Array(
        "As of 27 January 2025, your balance is £700.19 in debit.",
        "You have 4 overdue returns."
      )
    }

    "must open the make a payment link in a new tab and link to the existing pages" in {
      val app           = applicationBuilder().build()
      val view          = app.injector.instanceOf[ManageMachineGamesDutyView]
      implicit val msgs = messages(app)
      val doc           = Jsoup.parse(
        view(
          true,
          "Agent 1",
          "Client Business",
          "XWM00000001770",
          4,
          "27 January 2025",
          BigDecimal("700.19"),
          "debit"
        )(
          FakeRequest(),
          msgs
        ).body
      )

      val appConfig = app.injector.instanceOf[config.AppConfig]
      val payment   = doc.select("a:contains(Make a payment)").first()
      payment.attr("href") mustBe "https://www.gov.uk/pay-machine-games-duty"
      doc
        .select("a:contains(File a return)")
        .attr("href") mustBe s"${appConfig.filingFrontend}/XWM00000001770/select-return"
      doc.select("a:contains(Cancel registration)").attr("href") mustBe
        s"${appConfig.variationsFrontend}/cancel-registration/why-are-you-cancelling"
      doc
        .select("a:contains(View account statement)")
        .attr("href") mustBe s"${appConfig.landpFrontend}/statement/mgd/XWM00000001770/current"
      doc
        .select("a:contains(View filed returns)")
        .attr("href") mustBe "http://localhost:10401/manage-gambling-tax/returns/XWM00000001770/select-filed-return"
      payment.attr("target") mustBe "_blank"
      doc.select("a:contains(View registration certificate)").attr("href") mustBe
        controllers.routes.ViewRegistrationCertificateController.onPageLoad("XWM00000001770").url
      doc.select("a:contains(Manage your client details)").attr("href") mustBe
        controllers.clientdetails.routes.ManageClientDetailsController.onPageLoad().url
    }
  }

  "ManageMachineGamesDutyView (organisation)" - {
    "must not show the acting-on-behalf-of line or the Client details card" in {
      val app           = applicationBuilder().build()
      val view          = app.injector.instanceOf[ManageMachineGamesDutyView]
      implicit val msgs = messages(app)
      val doc           = Jsoup.parse(
        view(false, "", "", "XWM00000001770", 4, "27 January 2025", BigDecimal("700.19"), "debit")(
          FakeRequest(),
          msgs
        ).body
      )

      doc.select(".govuk-grid-column-full p.govuk-body").eachText().toArray mustBe
        Array("MGD registration number: XWM00000001770")
      doc.select(".card-heading").eachText().toArray mustBe Array("Statement", "Returns", "Registration")
    }
  }
}
