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
import pages.businessdetails.ChangeFaxNumberPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}

class FaxNumberSummarySpec extends SpecBase {

  private def changeFaxNumberSummaryUrl = controllers.businessdetails.routes.ChangeFaxNumberController.onPageLoad().url

  private def removeFaxNumberSummaryUrl = controllers.businessdetails.routes.RemoveFaxNumberController.onPageLoad().url

  "FaxNumberSummary" - {

    "must show a 'Change' link with no remove action when empty" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val rows = FaxNumberSummary.rows(emptyUserAnswers)
      val row = rows.find(_.key.content == Text(msgs("changeBusinessContact.faxNumber"))).value

      row.value.content mustBe Text(msgs("changeBusinessContact.notProvided"))
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeFaxNumberSummaryUrl
    }

    "must show the normal value with a change & remove action when value is entered" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val answers = emptyUserAnswers.set(ChangeFaxNumberPage, "01234567890123456789").success.value

      val rows = FaxNumberSummary.rows(answers)
      val row = rows.find(_.key.content == Text(msgs("changeBusinessContact.faxNumber"))).value

      row.value.content mustBe Text("01234567890123456789")
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeFaxNumberSummaryUrl

      row.actions.value.items(1).content mustBe Text(msgs("site.remove"))
      row.actions.value.items(1).href mustBe removeFaxNumberSummaryUrl
    }
  }
}
