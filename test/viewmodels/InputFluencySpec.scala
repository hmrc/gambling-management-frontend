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

package viewmodels.govuk

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.govukfrontend.views.viewmodels.input.Input
import viewmodels.govuk.input.FluentInput

class InputFluencySpec extends AnyFreeSpec with Matchers {

  "FluentInput" - {

    "must support asEmail" in {
      val result = Input().asEmail()

      result.inputType mustEqual "email"
      result.autocomplete mustEqual Some("email")
      result.spellcheck mustEqual Some(false)
    }

    "must support asNumeric" in {
      val result = Input().asNumeric()

      result.inputmode mustEqual Some("numeric")
      result.pattern mustEqual Some("[0-9]*")
    }

    "must support fluent modifiers" in {
      val result = Input()
        .withId("foo")
        .withInputType("email")
        .withInputMode("numeric")
        .describedBy("bar")
        .withCssClass("css")
        .withAutocomplete("email")
        .withPattern("[0-9]*")
        .withAttribute("data-test" -> "true")
        .withSpellcheck(false)

      result.id mustEqual "foo"
      result.inputType mustEqual "email"
      result.inputmode mustEqual Some("numeric")
      result.describedBy mustEqual Some("bar")
      result.classes must include("css")
      result.autocomplete mustEqual Some("email")
      result.pattern mustEqual Some("[0-9]*")
      result.attributes("data-test") mustEqual "true"
      result.spellcheck mustEqual Some(false)
    }
  }
}
