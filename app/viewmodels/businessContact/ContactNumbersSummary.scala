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

import models.UserAnswers
import models.agent.ContactNumbers
import pages.businessdetails.ChangeContactNumbersPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.{KeyViewModel, SummaryListRowViewModel, ValueViewModel}

object ContactNumbersSummary {

  def rows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {

    val contactNumbers =
      answers.get(ChangeContactNumbersPage)

    val changeContactNumbersUrl =
      controllers.businessdetails.routes.ChangeContactNumbersController
        .onPageLoad()
        .url

    val enterContactNumbersLink =
      HtmlContent(
        s"""<a class="govuk-link" href="$changeContactNumbersUrl">${messages(
            "changeBusinessContact.contactNumbers.empty"
          )}</a>"""
      )

    val row =
      contactNumbers match {

        case None =>
          SummaryListRowViewModel(
            key = KeyViewModel(
              Text(
                messages("changeBusinessContact.contactNumbers")
              )
            ),
            value = ValueViewModel(
              enterContactNumbersLink
            )
          )

        case Some(ContactNumbers(None, None)) =>
          SummaryListRowViewModel(
            key = KeyViewModel(
              Text(
                messages("changeBusinessContact.contactNumbers")
              )
            ),
            value = ValueViewModel(
              enterContactNumbersLink
            )
          )

        case Some(ContactNumbers(phoneNumber, mobileNumber)) =>
          BusinessContactHelpers.textOrActionLinkRowDouble(
            keyMsg = "changeBusinessContact.contactNumbers",
            answer = Some(
              Seq(
                messages("changeBusinessContact.contactNumbers.phone"),
                phoneNumber.getOrElse(
                  messages("changeBusinessContact.notProvided")
                ),
                "",
                messages("changeBusinessContact.contactNumbers.mobile"),
                mobileNumber.getOrElse(
                  messages("changeBusinessContact.notProvided")
                )
              ).mkString("<br>")
            ),
            urlChange = changeContactNumbersUrl,
            hiddenMsg = "changeBusinessContact.contactNumbers"
          )
      }

    Seq(row)
  }
}
