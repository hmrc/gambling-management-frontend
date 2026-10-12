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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.businessdetails.{ChangeEmailAddressPage, ChangeFaxNumberPage, RemoveFaxNumberPage}
import play.api.i18n.{Messages, MessagesImpl}
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}

class BusinessContactHelpersSpec extends AnyFreeSpec with Matchers {

  private val emptyAnswers = UserAnswers("id")

  private val messagesApi = stubMessagesApi(
    Map(
      "en" -> Map(
        "site.change" -> "Change",
        "site.remove" -> "Remove",
        "site.yes"    -> "Yes",
        "site.no"     -> "No",
        "some.key"    -> "Some key",
        "some.hidden" -> "Some hidden text",
        "some.link"   -> "Some link text"
      )
    )
  )

  implicit private val messages: Messages =
    MessagesImpl(play.api.i18n.Lang.defaultLang, messagesApi)

  ".hasChanges" - {
    "must be false when no contact details have been changed" in {
      BusinessContactHelpers.hasChanges(emptyAnswers) mustBe false
    }

    "must be true when the fax number has been changed" in {
      BusinessContactHelpers.hasChanges(emptyAnswers.set(ChangeFaxNumberPage, "123").get) mustBe true
    }

    "must be true when the email address has been changed" in {
      BusinessContactHelpers.hasChanges(emptyAnswers.set(ChangeEmailAddressPage, "someone@someone.com").get) mustBe true
    }

    "must be true when the fax number has been removed" in {
      BusinessContactHelpers.hasChanges(emptyAnswers.set(RemoveFaxNumberPage, true).get) mustBe true
    }

    "must be false when the user chose not to remove the fax number" in {
      BusinessContactHelpers.hasChanges(emptyAnswers.set(RemoveFaxNumberPage, false).get) mustBe false
    }
  }

  ".clearChanges" - {
    "must remove the submitted contact-detail pages" in {
      val answers = emptyAnswers
        .set(ChangeFaxNumberPage, "01881123456")
        .flatMap(_.set(ChangeEmailAddressPage, "someone@someone.com"))
        .get

      val cleared = BusinessContactHelpers.clearChanges(answers).get

      cleared.get(ChangeFaxNumberPage) mustBe None
      cleared.get(ChangeEmailAddressPage) mustBe None
      cleared.get(RemoveFaxNumberPage) mustBe None
    }

    "must succeed when there are no changes to clear" in {
      BusinessContactHelpers.clearChanges(emptyAnswers).isSuccess mustBe true
    }
  }

  ".textOrActionWithRemoveLinkRow" - {
    "must build a row with the not-provided text and only a change action when onlyShowChangeLink is true" in {
      val result = BusinessContactHelpers.textOrActionWithRemoveLinkRow(
        keyMsg = "some.key",
        answer = None,
        onlyShowChangeLink = true,
        notProvidedMsg = "some.link",
        urlChange = "/change-url",
        urlRemove = "/remove-url",
        hiddenMsg = "some.hidden"
      )

      result.key.content mustBe Text("Some key")
      result.value.content mustBe Text("Some link text")
      result.actions mustBe Some(
        Actions(items =
          Seq(ActionItem(href = "/change-url", content = Text("Change"), visuallyHiddenText = Some("Some hidden text")))
        )
      )
    }

    "must build a row with the given text and change & remove actions when onlyShowChangeLink is false" in {
      val result = BusinessContactHelpers.textOrActionWithRemoveLinkRow(
        keyMsg = "some.key",
        answer = Some("10"),
        onlyShowChangeLink = false,
        notProvidedMsg = "some.link",
        urlChange = "/change-url",
        urlRemove = "/remove-url",
        hiddenMsg = "some.hidden"
      )

      result.key.content mustBe Text("Some key")
      result.value.content mustBe Text("10")
      result.actions mustBe Some(
        Actions(items =
          Seq(
            ActionItem(href = "/change-url", content = Text("Change"), visuallyHiddenText = Some("Some hidden text")),
            ActionItem(href = "/remove-url", content = Text("Remove"), visuallyHiddenText = Some("Some hidden text"))
          )
        )
      )
    }
  }

  ".textOrActionLinkRowDouble" - {
    "must build a row with the given HTML value and a change action" in {
      val result = BusinessContactHelpers.textOrActionLinkRowDouble(
        keyMsg = "some.key",
        answer = Some("line one<br>line two"),
        urlChange = "/change-url",
        hiddenMsg = "some.hidden"
      )

      result.key.content mustBe Text("Some key")
      result.value.content mustBe HtmlContent("line one<br>line two")
      result.value.classes must include("govuk-!-width-one-half")
      result.actions mustBe Some(
        Actions(
          classes = "govuk-!-width-one-third",
          items = Seq(
            ActionItem(href = "/change-url", content = Text("Change"), visuallyHiddenText = Some("Some hidden text"))
          )
        )
      )
    }

    "must build a row with an empty value when the answer is None" in {
      val result = BusinessContactHelpers.textOrActionLinkRowDouble(
        keyMsg = "some.key",
        answer = None,
        urlChange = "/change-url",
        hiddenMsg = "some.hidden"
      )

      result.value.content mustBe HtmlContent("")
    }
  }
}
