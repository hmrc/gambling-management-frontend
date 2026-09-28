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

package viewmodels

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.{DefaultMessagesApi, Lang, MessagesImpl}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import viewmodels.implicits.*

class ImplicitConversionsSpec extends AnyFreeSpec with Matchers {

  private val messages = MessagesImpl(
    Lang("en"),
    new DefaultMessagesApi(
      Map(
        "en" -> Map("my.key" -> "My Label")
      )
    )
  )

  "stringToText" - {

    "must convert a message key to a Text content" in {
      val result: Text = stringToText("my.key")(messages)
      result mustEqual Text("My Label")
    }

    "must use the raw string when no message is found" in {
      val result: Text = stringToText("unknown.key")(messages)
      result mustEqual Text("unknown.key")
    }
  }

  "stringToKey" - {

    "must convert a message key to a Key with Text content" in {
      val result: Key = stringToKey("my.key")(messages)
      result mustEqual Key(content = Text("My Label"))
    }
  }
}
