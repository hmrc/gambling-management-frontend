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

//  private def removeClientReferenceUrl =
//    controllers.businessdetails.routes.RemoveClientReferenceController.onPageLoad(regNumber).url //TODO :uncomment to use correct url

  private val clientName      = "ABC Ltd"
  private val regNumber       = "XVM00000001001"
  private val clientReference = "EZ00100"

  "ClientDetailsSummary" - {

    "must show a clientName row with an empty Action" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val rows = ClientDetailsSummary.rows(clientName, regNumber, clientReference).rows
      val row  = rows.find(_.key.content == Text(msgs("manageClientDetails.clientName"))).value

      row.value.content mustBe Text(msgs("ABC Ltd"))
      row.actions.value.items.head.content mustBe Text(msgs(""))
      row.actions.value.items.head.href mustBe "#"
      row.actions.value.classes mustBe "govuk-!-width-one-third"
    }

    "must show a regNumber row with NO Action" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val rows = ClientDetailsSummary.rows(clientName, regNumber, clientReference).rows
      val row  = rows.find(_.key.content == Text(msgs("manageClientDetails.regNumber"))).value

      row.value.content mustBe Text(msgs("XVM00000001001"))
      row.actions mustBe None
    }

    "must show a clientReference row with a 'Change' Action with no remove action when clientReference is SPACES" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val rows = ClientDetailsSummary.rows(clientName, regNumber, "      ").rows
      val row  = rows.find(_.key.content == Text(msgs("manageClientDetails.clientReference"))).value

      row.value.content mustBe Text(msgs("manageClientDetails.notProvided"))
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeClientReferenceUrl(regNumber)
      row.actions.value.items.size mustBe 1
    }

    "must show a clientReference row with with a change & remove action when value is entered" in {
      implicit val msgs: Messages = messages(applicationBuilder().build())

      val rows = ClientDetailsSummary.rows(clientName, regNumber, clientReference).rows
      val row  = rows.find(_.key.content == Text(msgs("manageClientDetails.clientReference"))).value

      row.value.content mustBe Text("EZ00100")
      row.actions.value.items.size mustBe 2
      row.actions.value.items.head.content mustBe Text(msgs("site.change"))
      row.actions.value.items.head.href mustBe changeClientReferenceUrl(regNumber)

      row.actions.value.items(1).content mustBe Text(msgs("site.remove"))
      row.actions.value.items(1).href mustBe "#"
    }
  }
}
