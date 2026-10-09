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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, SummaryListRow}
import viewmodels.govuk.summarylist.*
import views.CurrencyFormatter

object BusinessContactHelpers {

  def yesNoRow(keyMsg: String, answer: Boolean, changeUrl: String, hiddenMsg: String)(implicit
                                                                                      messages: Messages
  ): SummaryListRow = {
    val msg = if (answer) "site.yes" else "site.no"
    SummaryListRowViewModel(
      key = KeyViewModel(Text(messages(keyMsg))).withCssClass("govuk-!-width-one-half"),
      value = ValueViewModel(Text(messages(msg))).withCssClass("govuk-!-text-align-right"),
      actions = Seq(
        ActionItemViewModel(Text(messages("site.change")), changeUrl)
          .withVisuallyHiddenText(messages(hiddenMsg))
      )
    )
  }

  def currencyRow(
                   keyMsg: String,
                   amount: BigDecimal,
                   changeUrl: Option[String] = None,
                   hiddenMsg: Option[String] = None
                 )(implicit messages: Messages): SummaryListRow = {
    val summary = SummaryListRowViewModel(
      key = KeyViewModel(Text(messages(keyMsg))).withCssClass("govuk-!-width-one-half"),
      value = ValueViewModel(HtmlContent(CurrencyFormatter.formattedAmountHtml(amount)))
        .withCssClass("govuk-!-text-align-right")
    )

    (changeUrl, hiddenMsg) match
      case (Some(url), Some(hidden)) if url.trim.nonEmpty && hidden.trim.nonEmpty =>
        summary.copy(actions =
          Some(
            Actions(items =
              Seq(ActionItemViewModel(Text(messages("site.change")), url).withVisuallyHiddenText(messages(hidden)))
            )
          )
        )
      case _ => summary
  }

  def textRow(
               keyMsg: String,
               answer: String,
               changeUrl: Option[String] = None,
               hiddenMsg: Option[String] = None
             )(implicit messages: Messages): SummaryListRow = {
    val summary = SummaryListRowViewModel(
      key = KeyViewModel(Text(messages(keyMsg))).withCssClass("govuk-!-width-one-half"),
      value = ValueViewModel(Text(answer)).withCssClass("govuk-!-text-align-right")
    )

    (changeUrl, hiddenMsg) match
      case (Some(url), Some(hidden)) if url.trim.nonEmpty && hidden.trim.nonEmpty =>
        summary.copy(actions =
          Some(
            Actions(items =
              Seq(ActionItemViewModel(Text(messages("site.change")), url).withVisuallyHiddenText(messages(hidden)))
            )
          )
        )
      case _ => summary
  }

  private def valueOrActionLinkRow(
                                    keyMsg: String,
                                    normalValueContent: Content,
                                    showValueLink: Boolean,
                                    linkTextMsg: String,
                                    url: String,
                                    hiddenMsg: String
                                  )(implicit messages: Messages): SummaryListRow =
    if (showValueLink)
      SummaryListRowViewModel(
        key = KeyViewModel(Text(messages(keyMsg))).withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(HtmlContent(s"""<a class="govuk-link" href="$url">${messages(linkTextMsg)}</a>"""))
          .withCssClass("govuk-!-text-align-right")
      )
    else
      SummaryListRowViewModel(
        key = KeyViewModel(Text(messages(keyMsg))).withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(normalValueContent).withCssClass("govuk-!-text-align-right"),
        actions = Seq(
          ActionItemViewModel(Text(messages("site.change")), url)
            .withVisuallyHiddenText(messages(hiddenMsg))
        )
      )

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
    SummaryListRowViewModel(
      key = KeyViewModel(Text(messages(keyMsg))),
      value = ValueViewModel(HtmlContent(normalValueContent)),
      actions = Seq(
        ActionItemViewModel(Text(messages("site.change")), urlChange).withVisuallyHiddenText(messages(hiddenMsg))
      )
    )

  def currencyOrActionLinkRow(
                               keyMsg: String,
                               amount: BigDecimal,
                               showValueLink: Boolean,
                               linkTextMsg: String,
                               url: String,
                               hiddenMsg: String
                             )(implicit messages: Messages): SummaryListRow =
    valueOrActionLinkRow(
      keyMsg,
      HtmlContent(CurrencyFormatter.formattedAmountHtml(amount)),
      showValueLink,
      linkTextMsg,
      url,
      hiddenMsg
    )

  def yesNoOrActionLinkRow(
                            keyMsg: String,
                            answer: Option[Boolean],
                            showValueLink: Boolean,
                            linkTextMsg: String,
                            url: String,
                            hiddenMsg: String
                          )(implicit messages: Messages): SummaryListRow =
    valueOrActionLinkRow(
      keyMsg,
      Text(answer.map(a => messages(if (a) "site.yes" else "site.no")).getOrElse("")),
      showValueLink,
      linkTextMsg,
      url,
      hiddenMsg
    )

  def textOrActionLinkRow(
                           keyMsg: String,
                           answer: Option[String],
                           showValueLink: Boolean,
                           linkTextMsg: String,
                           url: String,
                           hiddenMsg: String
                         )(implicit messages: Messages): SummaryListRow =
    valueOrActionLinkRow(
      keyMsg,
      Text(answer.getOrElse("")),
      showValueLink,
      linkTextMsg,
      url,
      hiddenMsg
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
