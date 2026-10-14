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
import pages.businessdetails.{ChangeContactNumbersPage, ChangeEmailAddressPage, ChangeFaxNumberPage, RemoveFaxNumberPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, SummaryListRow}
import viewmodels.govuk.summarylist.*

import scala.util.Try

object BusinessContactHelpers {

  def hasChanges(answers: UserAnswers): Boolean =
    answers.get(ChangeFaxNumberPage).isDefined ||
      answers.get(ChangeEmailAddressPage).isDefined ||
      answers.get(ChangeContactNumbersPage).isDefined ||
      answers.get(RemoveFaxNumberPage).contains(true)

  /** Clears the contact-detail changes once they have been submitted to CHRIS, leaving the rest of the answers intact.
    */
  def clearChanges(answers: UserAnswers): Try[UserAnswers] =
    answers
      .remove(ChangeFaxNumberPage)
      .flatMap(_.remove(ChangeEmailAddressPage))
      .flatMap(_.remove(RemoveFaxNumberPage))
      .flatMap(_.remove(ChangeContactNumbersPage))

  private def valueOrActionWithRemoveLinkRow(
    keyMsg: String,
    normalValueContent: Content,
    onlyShowChangeLink: Boolean,
    notProvidedMsg: String,
    urlChange: String,
    urlRemove: String,
    hiddenMsg: String
  )(implicit messages: Messages): SummaryListRow =
    if (onlyShowChangeLink)
      SummaryListRowViewModel(
        key = KeyViewModel(Text(messages(keyMsg))),
        value = ValueViewModel(Text(messages(notProvidedMsg))),
        actions = Seq(
          ActionItemViewModel(Text(messages("site.change")), urlChange)
            .withVisuallyHiddenText(messages(hiddenMsg))
        )
      )
    else
      SummaryListRowViewModel(
        key = KeyViewModel(Text(messages(keyMsg))),
        value = ValueViewModel(normalValueContent),
        actions = Seq(
          ActionItemViewModel(Text(messages("site.change")), urlChange)
            .withVisuallyHiddenText(messages(hiddenMsg)),
          ActionItemViewModel(Text(messages("site.remove")), urlRemove)
            .withVisuallyHiddenText(messages(hiddenMsg))
        )
      )

  private def valueOrActionLinkRowDouble(
    keyMsg: String,
    normalValueContent: String,
    urlChange: String,
    hiddenMsg: String
  )(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = KeyViewModel(Text(messages(keyMsg))),
      value = ValueViewModel(HtmlContent(normalValueContent)).withCssClass("govuk-!-width-one-half"),
      actions = Some(
        Actions(
          classes = "govuk-!-width-one-third",
          items = Seq(
            ActionItemViewModel(Text(messages("site.change")), urlChange).withVisuallyHiddenText(messages(hiddenMsg))
          )
        )
      )
    )

  def textOrActionWithRemoveLinkRow(
    keyMsg: String,
    answer: Option[String],
    onlyShowChangeLink: Boolean,
    notProvidedMsg: String,
    urlChange: String,
    urlRemove: String,
    hiddenMsg: String
  )(implicit messages: Messages): SummaryListRow =
    valueOrActionWithRemoveLinkRow(
      keyMsg,
      Text(answer.getOrElse("")),
      onlyShowChangeLink,
      notProvidedMsg,
      urlChange,
      urlRemove,
      hiddenMsg
    )

  def textOrActionLinkRowDouble(
    keyMsg: String,
    answer: Option[String],
    urlChange: String,
    hiddenMsg: String
  )(implicit messages: Messages): SummaryListRow =
    valueOrActionLinkRowDouble(
      keyMsg,
      answer.getOrElse(""),
      urlChange,
      hiddenMsg
    )
}
