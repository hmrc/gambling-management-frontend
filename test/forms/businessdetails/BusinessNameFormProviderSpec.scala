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

package forms.businessdetails

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError

class BusinessNameFormProviderSpec extends AnyFreeSpec with Matchers with OptionValues {

  private val form = new BusinessNameFormProvider()()

  "BusinessNameFormProvider" - {

    "must trim and collapse multiple spaces to a single space" in {
      form.bind(Map("value" -> "  abc    def ")).value.value mustBe "abc def"
    }

    "must keep single spaces between words" in {
      form.bind(Map("value" -> "abc def ghi")).value.value mustBe "abc def ghi"
    }

    "must collapse tabs and other whitespace to a single space" in {
      form.bind(Map("value" -> "abc\t\t def")).value.value mustBe "abc def"
    }

    "must collapse non-breaking spaces to a single space" in {
      form.bind(Map("value" -> "\u00A0abc\u00A0\u00A0def\u00A0")).value.value mustBe "abc def"
    }

    "must return the required error when only spaces are entered" in {
      form.bind(Map("value" -> "    ")).errors mustBe Seq(FormError("value", "businessName.error.required"))
    }
  }
}
