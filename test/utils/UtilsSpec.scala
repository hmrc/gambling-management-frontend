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

package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

class UtilsSpec extends AnyWordSpec with Matchers {

  "Utils.emptyString" should {

    "return an empty string" in {

      Utils.emptyString shouldBe ""
    }
  }

  "Utils.firstRadioId" should {

    "return value_0" in {

      Utils.firstRadioId shouldBe "value_0"
    }
  }

  "Utils.withIds" should {

    "add sequential ids using default prefix" in {

      val items = Seq(
        RadioItem(content = Text("Option 1")),
        RadioItem(content = Text("Option 2")),
        RadioItem(content = Text("Option 3"))
      )

      val result = Utils.withIds(items)

      result.map(_.id) shouldBe Seq(
        Some("value_0"),
        Some("value_1"),
        Some("value_2")
      )
    }

    "add sequential ids using custom prefix" in {

      val items = Seq(
        RadioItem(content = Text("Option 1")),
        RadioItem(content = Text("Option 2"))
      )

      val result = Utils.withIds(items, prefix = "custom")

      result.map(_.id) shouldBe Seq(
        Some("custom_0"),
        Some("custom_1")
      )
    }

    "preserve other RadioItem fields" in {

      val items = Seq(
        RadioItem(
          content = Text("Option 1"),
          value = Some("1"),
          checked = true
        )
      )

      val result = Utils.withIds(items)

      result.head.content shouldBe Text("Option 1")
      result.head.value   shouldBe Some("1")
      result.head.checked shouldBe true
      result.head.id      shouldBe Some("value_0")
    }

    "return empty sequence when given empty sequence" in {

      Utils.withIds(Seq.empty) shouldBe Seq.empty
    }

    "overwrite existing ids" in {

      val items = Seq(
        RadioItem(
          content = Text("Option 1"),
          id = Some("old_id")
        )
      )

      val result = Utils.withIds(items)

      result.head.id shouldBe Some("value_0")
    }
  }
}
