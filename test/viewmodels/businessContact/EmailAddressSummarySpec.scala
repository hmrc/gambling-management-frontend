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
import models.CheckMode
import pages.businessdetails.ChangeEmailAddressPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}

class EmailAddressSummarySpec extends SpecBase {

  private def changeEmailAddressSummaryUrl = controllers.businessdetails.routes.ChangeEmailAddressController.onPageLoad().url

  private def removeEmailAddressSummaryUrl = controllers.businessdetails.routes.ChangeEmailAddressController.onPageLoad().url

  "EmailAddressSummary" - {

    "must show a 'Change' link with no remove action when empty" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val rows = EmailAddressSummary.rows(emptyUserAnswers)
      val row = rows.find(_.key.content == Text(msgs("changeBusinessContact.emailAddress"))).value

      row.value.content mustBe Text(msgs("changeBusinessContact.notProvided"))
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeEmailAddressSummaryUrl
    }

    "must show the normal value with a change & remove action when value is entered" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val answers = emptyUserAnswers.set(ChangeEmailAddressPage, "01234567890123456789@01234567890123456789.co.uk").success.value

      val rows = EmailAddressSummary.rows(answers)
      val row = rows.find(_.key.content == Text(msgs("changeBusinessContact.emailAddress"))).value

      row.value.content mustBe Text("01234567890123456789@01234567890123456789.co.uk")
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeEmailAddressSummaryUrl

      row.actions.value.items(1).content mustBe Text(msgs("site.remove"))
      row.actions.value.items(1).href mustBe removeEmailAddressSummaryUrl
    }
  }
}
