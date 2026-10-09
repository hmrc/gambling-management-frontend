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

package viewmodels.businessContact

import base.SpecBase
import controllers.routes
import models.{BusinessDetails, CheckMode}
import pages.BusinessDetailsPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}

class ContactNumbersSummarySpec extends SpecBase {

  private def changeContactNumbersSummaryUrl = controllers.businessdetails.routes.ChangeFaxNumberController.onPageLoad().url

  "ContactNumbersSummary" - {

    "must show a 'Change' link with no remove action when empty" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val rows = ContactNumbersSummary.rows(emptyUserAnswers)
      val row = rows.find(_.key.content == Text(msgs("changeBusinessContact.contactNumbers"))).value

      row.value.content mustBe HtmlContent(msgs("Phone number:<br>Not provided<br><br>Mobile number:<br>Not provided"))
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeContactNumbersSummaryUrl
    }

    "must show the normal value with a change & remove action when phone value is entered" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val answers = emptyUserAnswers.set(BusinessDetailsPage, BusinessDetails(
        None, None, None, None, phoneNumber = Some("01234567890123456789"), None, None, None)).success.value

      val rows = ContactNumbersSummary.rows(answers)
      val row = rows.find(_.key.content == Text(msgs("changeBusinessContact.contactNumbers"))).value

      row.value.content mustBe HtmlContent("Phone number:<br>01234567890123456789<br><br>Mobile number:<br>Not provided")
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeContactNumbersSummaryUrl
    }

    "must show the normal value with a change & remove action when mobile value is entered" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val answers = emptyUserAnswers.set(BusinessDetailsPage, BusinessDetails(
        None, None, None, None, None, mobileNumber = Some("01234567890123456789"), None, None)).success.value

      val rows = ContactNumbersSummary.rows(answers)
      val row = rows.find(_.key.content == Text(msgs("changeBusinessContact.contactNumbers"))).value

      row.value.content mustBe HtmlContent("Phone number:<br>Not provided<br><br>Mobile number:<br>01234567890123456789")
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeContactNumbersSummaryUrl
    }

    "must show the normal value with a change & remove action when phone & mobile value is entered" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val answers = emptyUserAnswers.set(BusinessDetailsPage, BusinessDetails(
        None, None, None, None, phoneNumber = Some("01234567890123456789"), mobileNumber = Some("98765432109876543210"), None, None)).success.value

      val rows = ContactNumbersSummary.rows(answers)
      val row = rows.find(_.key.content == Text(msgs("changeBusinessContact.contactNumbers"))).value

      row.value.content mustBe HtmlContent("Phone number:<br>01234567890123456789<br><br>Mobile number:<br>98765432109876543210")
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeContactNumbersSummaryUrl
    }
  }
}
