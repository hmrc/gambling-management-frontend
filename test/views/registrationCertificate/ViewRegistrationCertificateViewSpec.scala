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

package views.registrationCertificate

import base.SpecBase
import models.BusinessType.Soleproprietor
import models.{GroupMember, MgdCertificate, PartnerMember, ReturnPeriodEndDate}
import org.jsoup.Jsoup
import play.api.test.FakeRequest

import java.time.LocalDate

class ViewRegistrationCertificateViewSpec extends SpecBase {

  private def baseCertificate =
    MgdCertificate(
      mgdRegNumber = "MGD123",
      registrationDate = Some(LocalDate.parse("2026-01-01")),
      individualName = Some("John Doe"),
      businessName = Some("Test Business Ltd"),
      tradingName = None,
      repMemName = None,
      busAddrLine1 = Some("Line 1"),
      busAddrLine2 = Some("Line 2"),
      busAddrLine3 = None,
      busAddrLine4 = None,
      busPostcode = Some("AB1 2CD"),
      busCountry = None,
      busAdi = None,
      repMemLine1 = None,
      repMemLine2 = None,
      repMemLine3 = None,
      repMemLine4 = None,
      repMemPostcode = None,
      repMemAdi = None,
      typeOfBusiness = Some("Corporate Body"),
      businessTradeClass = Some(1),
      noOfPartners = None,
      groupReg = "N",
      noOfGroupMems = None,
      dateCertIssued = Some(LocalDate.parse("2026-01-02")),
      partMembers = Seq.empty,
      groupMembers = Seq.empty,
      returnPeriodEndDates = Seq(
        ReturnPeriodEndDate(LocalDate.parse("2026-12-31"))
      )
    )

  "ViewRegistrationCertificateView" - {

    "must render core certificate details correctly" in {

      val app     = applicationBuilder().build()
      val view    = app.injector.instanceOf[views.html.ViewRegistrationCertificateView]
      val request = FakeRequest()

      val cert = baseCertificate

      val displayName     = cert.businessName.getOrElse("")
      val displayLabelKey = "viewRegistrationCertificate.label.corporateBody"

      val formattedAddress =
        Seq(
          cert.busAddrLine1,
          cert.busAddrLine2,
          cert.busAddrLine3,
          cert.busAddrLine4,
          cert.busPostcode
        ).flatten.mkString("<br>")

      val html = view(cert, displayName, displayLabelKey, formattedAddress)(request, messages(app))
      val doc  = Jsoup.parse(html.body)

      doc.title()                           must include(messages(app)("viewRegistrationCertificate.title"))
      println("??? + " + doc.select(".govuk-panel__body").text)
      doc.select(".govuk-panel__body").text must include("MGD123")

      val pageText = doc.body().text()

      pageText must include("Test Business Ltd")
      pageText must include("Corporate Body")
      pageText must include("Line 1")
      pageText must include("Line 2")
      pageText must include("AB1 2CD")

      doc.select(".govuk-list li").text must include("31 Dec 2026")
      doc.select("a[href='#']").attr("href") mustBe "#"
    }

    "must render partnership section when partners exist" in {

      val app     = applicationBuilder().build()
      val view    = app.injector.instanceOf[views.html.ViewRegistrationCertificateView]
      val request = FakeRequest()

      val cert = baseCertificate.copy(
        partMembers = Seq(
          PartnerMember(Some("Partner A"), None, None, None, None, Soleproprietor),
          PartnerMember(Some("Partner B"), None, None, None, None, Soleproprietor)
        )
      )

      val html = view(cert, "Test Business Ltd", "viewRegistrationCertificate.label.partnership", "")(
        request,
        messages(app)
      )
      val doc  = Jsoup.parse(html.body)

      val pageText = doc.body().text()

      pageText must include(messages(app)("viewRegistrationCertificate.partnershipDetails"))
      pageText must include("Partner A")
      pageText must include("Partner B")
    }

    "must render group registration details and group members separately" in {

      val app     = applicationBuilder().build()
      val view    = app.injector.instanceOf[views.html.ViewRegistrationCertificateView]
      val request = FakeRequest()

      val cert = baseCertificate.copy(
        groupReg = "Y",
        repMemName = Some("Rep Member Ltd"),
        repMemLine1 = Some("Rep Line 1"),
        repMemPostcode = Some("ZZ1 1ZZ"),
        groupMembers = Seq(
          GroupMember("Group A"),
          GroupMember("Group B")
        )
      )

      val html = view(cert, "Test Business Ltd", "viewRegistrationCertificate.label.corporateBody", "")(
        request,
        messages(app)
      )
      val doc  = Jsoup.parse(html.body)

      val pageText = doc.body().text()

      // Group registration section
      pageText must include(messages(app)("viewRegistrationCertificate.groupDetails"))
      pageText must include("Rep Member Ltd")
      pageText must include("Rep Line 1")
      pageText must include("ZZ1 1ZZ")

      // Separate group members section
      pageText must include(messages(app)("viewRegistrationCertificate.groupMembers"))
      pageText must include("Group A")
      pageText must include("Group B")
    }

    "must not render group members section when no members exist" in {

      val app     = applicationBuilder().build()
      val view    = app.injector.instanceOf[views.html.ViewRegistrationCertificateView]
      val request = FakeRequest()

      val cert = baseCertificate.copy(
        groupReg = "Y",
        groupMembers = Seq.empty
      )

      val html = view(cert, "Test Business Ltd", "viewRegistrationCertificate.label.corporateBody", "")(
        request,
        messages(app)
      )
      val doc  = Jsoup.parse(html.body)

      val pageText = doc.body().text()

      pageText must not include messages(app)("viewRegistrationCertificate.groupMembers")
    }

    "must display correct trade class label for businessTradeClass" in {

      val app     = applicationBuilder().build()
      val view    = app.injector.instanceOf[views.html.ViewRegistrationCertificateView]
      val request = FakeRequest()

      val cert = baseCertificate.copy(
        businessTradeClass = Some(6)
      )

      val html = view(cert, "Test Business Ltd", "viewRegistrationCertificate.label.corporateBody", "")(
        request,
        messages(app)
      )
      val doc  = Jsoup.parse(html.body)

      val pageText = doc.body().text()

      pageText must include("Casino")
    }

  }
}
