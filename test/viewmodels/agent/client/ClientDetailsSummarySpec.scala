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

package viewmodels.agent.client

import base.SpecBase
import controllers.routes
import models.{BusinessDetails, CheckMode}
import pages.BusinessDetailsPage
import pages.businessdetails.{ChangeEmailAddressPage, RemoveEmailAddressPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}

class ClientDetailsSummarySpec extends SpecBase {

  private def changeClientReferenceUrl(regNumber: String) =
    controllers.clientdetails.routes.ChangeClientReferenceController.onPageLoad(regNumber).url

  private def removeClientReferenceUrl =
    controllers.businessdetails.routes.RemoveEmailAddressController.onPageLoad().url //TODO :use correct url
    
  private val clientName = "ABC Ltd"
  private val regNumber = "XVM00000001001"
  private val clientReference = "EZ00100"

  "ClientDetailsSummary" - {

    "must show a 'Change' link with no remove action when empty" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val rows = ClientDetailsSummary.rows(clientName, regNumber, clientReference)
      val row  = rows.find(_.key.content == Text(msgs("changeBusinessContact.emailAddress"))).value

      row.value.content mustBe Text(msgs("changeBusinessContact.notProvided"))
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeClientReferenceUrl
    }

    "must show the normal value with a change & remove action when value is entered" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val answers =
        emptyUserAnswers.set(ChangeEmailAddressPage, "01234567890123456789@01234567890123456789.co.uk").success.value

      val rows = ClientDetailsSummary.rows(clientName, regNumber, clientReference)
      val row  = rows.find(_.key.content == Text(msgs("changeBusinessContact.emailAddress"))).value

      row.value.content mustBe Text("01234567890123456789@01234567890123456789.co.uk")
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeClientReferenceUrl

      row.actions.value.items(1).content mustBe Text(msgs("site.remove"))
      row.actions.value.items(1).href mustBe removeClientReferenceUrl
    }

    "must show 'Not provided' with only a 'Change' link when the email has been removed, even though the change page still holds the old value" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val answers =
        emptyUserAnswers
          .set(ChangeEmailAddressPage, "someone@example.com")
          .success
          .value
          .set(RemoveEmailAddressPage, true)
          .success
          .value

      val rows = ClientDetailsSummary.rows(clientName, regNumber, clientReference)
      val row  = rows.find(_.key.content == Text(msgs("changeBusinessContact.emailAddress"))).value

      row.value.content mustBe Text(msgs("changeBusinessContact.notProvided"))
      row.actions.value.items.size mustBe 1
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeClientReferenceUrl
    }
  }
}
