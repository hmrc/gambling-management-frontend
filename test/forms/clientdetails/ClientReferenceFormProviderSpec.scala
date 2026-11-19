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

package forms.clientdetails

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ClientReferenceFormProviderSpec extends AnyFreeSpec with Matchers with OptionValues {

  private val form = new ClientReferenceFormProvider()()

  "ClientReferenceFormProvider" - {

    "must trim leading and trailing spaces" in {
      form.bind(Map("value" -> "  AB123  ")).value.value mustBe "AB123"
    }

    "must reject a client reference with spaces in between" in {
      form.bind(Map("value" -> "AB   123")).errors.map(_.message) mustBe Seq("agent.clientReference.error.invalid")
    }
  }
}
