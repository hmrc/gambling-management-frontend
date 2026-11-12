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

import models.UserAnswers
import pages.BusinessDetailsPage
import pages.businessdetails.ChangeEmailAddressPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, SummaryList, SummaryListRow}
import viewmodels.businessContact.BusinessContactHelpers
import viewmodels.govuk.summarylist.{ActionItemViewModel, FluentActionItem, FluentKey, FluentValue, KeyViewModel, SummaryListRowViewModel, SummaryListViewModel, ValueViewModel}

object ClientDetailsSummary {

  def rows(clientName: String, regNumber: String, clientReference: String)(implicit
    messages: Messages
  ): SummaryList =

    val clientNameRow = SummaryListRow(
      key = KeyViewModel(Text(messages("agent.manageClientDetails.clientName"))),
      value = ValueViewModel(Text(clientName)),
      // need to add an empty Action with "govuk-!-width-one-third" css class so the clientReferenceRow displays the Change|Remove correctly
      actions = Some(
        Actions(
          classes = "govuk-!-width-one-third",
          items = Seq(
            ActionItemViewModel(Text(""), "#").withVisuallyHiddenText(messages(""))
          )
        )
      )
    )

    val regNumberRow = SummaryListRow(
      key = KeyViewModel(Text(messages("agent.manageClientDetails.regNumber"))),
      value = ValueViewModel(Text(regNumber))
    )

    val clientReferenceRow =
      if (clientReference.trim.isEmpty)
        SummaryListRow(
          key = KeyViewModel(Text(messages("agent.manageClientDetails.clientReference"))),
          value = ValueViewModel(Text(messages("agent.manageClientDetails.notProvided"))),
          actions = Some(
            Actions(
              items = Seq(
                ActionItemViewModel(
                  Text(messages("site.change")),
                  controllers.clientdetails.routes.ChangeClientReferenceController.onPageLoad(regNumber).url
                ).withVisuallyHiddenText(messages("agent.manageClientDetails.clientReference"))
              )
            )
          )
        )
      else
        SummaryListRow(
          key = KeyViewModel(Text(messages("agent.manageClientDetails.clientReference"))),
          value = ValueViewModel(Text(Some(clientReference).getOrElse(""))),
          actions = Some(
            Actions(
              classes = "govuk-!-width-one-third",
              items = Seq(
                ActionItemViewModel(
                  Text(messages("site.change")),
                  controllers.clientdetails.routes.ChangeClientReferenceController.onPageLoad(regNumber).url
                ).withVisuallyHiddenText(messages("agent.manageClientDetails.clientReference")),
                ActionItemViewModel(
                  Text(messages("site.remove")),
                  controllers.clientdetails.routes.RemoveClientReferenceController.onPageLoad(regNumber).url
                ).withVisuallyHiddenText(messages("agent.manageClientDetails.clientReference"))
              )
            )
          )
        )

    SummaryListViewModel(Seq(clientNameRow, regNumberRow, clientReferenceRow))
}
