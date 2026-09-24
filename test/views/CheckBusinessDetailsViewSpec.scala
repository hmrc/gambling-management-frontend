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
import models.BusinessDetails
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest

import scala.jdk.CollectionConverters.*

class CheckBusinessDetailsViewSpec extends SpecBase {

  private val details = BusinessDetails(
    businessName = Some("Agent 1"),
    addressLine1 = Some("123 Business road"),
    addressLine2 = Some("Business"),
    addressLine3 = Some("London"),
    phoneNumber  = Some("0191 202 2500"),
    mobileNumber = Some("07890 123 456"),
    faxNumber    = Some("0800 202 2500"),
    emailAddress = Some("sarah.phillips@example.com")
  )

  private def rowFor(doc: Document, keyText: String): Element =
    doc
      .select(".govuk-summary-list__row")
      .asScala
      .find(_.select(".govuk-summary-list__key").text() == keyText)
      .getOrElse(fail(s"could not find a row with key '$keyText'"))

  "CheckBusinessDetailsView" - {

    "must render the page title and heading" in {
      val app = applicationBuilder().build()
      val view = app.injector.instanceOf[views.html.CheckBusinessDetailsView]
      val request = FakeRequest()
      implicit val msgs = messages(app)

      val doc = Jsoup.parse(view(details)(request, msgs).body)

      doc.title() must include(msgs("checkBusinessDetails.title"))
      doc.select("h1").text() mustEqual msgs("checkBusinessDetails.heading")
    }

    "must render each row with its value and a Change link" in {
      val app = applicationBuilder().build()
      val view = app.injector.instanceOf[views.html.CheckBusinessDetailsView]
      val request = FakeRequest()
      implicit val msgs = messages(app)

      val doc = Jsoup.parse(view(details)(request, msgs).body)

      def valueFor(keyText: String): String =
        rowFor(doc, keyText).select(".govuk-summary-list__value").text()

      valueFor(msgs("checkBusinessDetails.businessName")) mustEqual "Agent 1"
      valueFor(msgs("checkBusinessDetails.businessAddress")) mustEqual "123 Business road, Business, London"
      valueFor(msgs("checkBusinessDetails.contactDetails.heading")) mustEqual
        "Phone number: 0191 202 2500 Mobile number: 07890 123 456 Fax number: 0800 202 2500 Email address: sarah.phillips@example.com"

      rowFor(doc, msgs("checkBusinessDetails.businessName"))
        .select(".govuk-summary-list__actions a")
        .text() mustEqual s"""${msgs("site.change")} ${msgs("checkBusinessDetails.businessName")}"""
      rowFor(doc, msgs("checkBusinessDetails.contactDetails.heading"))
        .select(".govuk-summary-list__actions a")
        .text() mustEqual s"""${msgs("site.change")} ${msgs("checkBusinessDetails.contactDetails.heading")}"""
    }

    "must render missing optional fields as empty" in {
      val app = applicationBuilder().build()
      val view = app.injector.instanceOf[views.html.CheckBusinessDetailsView]
      implicit val msgs = messages(app)

      val doc = Jsoup.parse(view(details.copy(businessName = None, faxNumber = None))(FakeRequest(), msgs).body)

      rowFor(doc, msgs("checkBusinessDetails.businessName")).select(".govuk-summary-list__value").text() mustEqual ""
      rowFor(doc, msgs("checkBusinessDetails.contactDetails.heading")).select(".govuk-summary-list__value").text() must include(
        "Fax number: Email address:"
      )
    }

    "must render the Continue button as a submit for the onSubmit route" in {
      val app = applicationBuilder().build()
      val view = app.injector.instanceOf[views.html.CheckBusinessDetailsView]
      val request = FakeRequest()
      implicit val msgs = messages(app)

      val doc = Jsoup.parse(view(details)(request, msgs).body)

      val form = doc.select("form")
      form.attr("action") mustEqual controllers.routes.CheckBusinessDetailsController.onSubmit().url

      val button = form.select("button.govuk-button")
      button.text() mustEqual msgs("site.continue")
    }
  }
}
