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

class ChangeFaxNumberFormProviderSpec extends AnyFreeSpec with Matchers with OptionValues {

  private val form = new ChangeFaxNumberFormProvider()()

  "ChangeFaxNumberFormProvider" - {

    "must remove all spaces" in {
      form.bind(Map("value" -> "  0123   456 789 ")).value.value mustBe "0123456789"
    }

    "must remove tabs, non-breaking and other Unicode spaces" in {
      form.bind(Map("value" -> " 0123\t456 789 ")).value.value mustBe "0123456789"
    }

    "must return the required error when only spaces are entered" in {
      form.bind(Map("value" -> "   ")).errors.map(_.message) mustBe Seq("changeFaxNumber.error.required")
    }
  }
}
