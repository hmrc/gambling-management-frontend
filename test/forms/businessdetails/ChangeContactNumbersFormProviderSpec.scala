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

import models.agent.ContactNumbers
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ChangeContactNumbersFormProviderSpec extends AnyFreeSpec with Matchers with OptionValues {

  private val form = new ChangeContactNumbersFormProvider()()

  "ChangeContactNumbersFormProvider" - {

    "must trim and collapse multiple spaces to a single space in the phone number" in {
      form.bind(Map("phoneNumber" -> "  0123    456 789 ")).value.value.phoneNumber.value mustBe "0123 456 789"
    }

    "must trim and collapse multiple spaces to a single space in the mobile number" in {
      form.bind(Map("mobileNumber" -> " 07700    900123  ")).value.value.mobileNumber.value mustBe "07700 900123"
    }

    "must collapse non-breaking spaces to a single space" in {
      form.bind(Map("phoneNumber" -> "\u00A00123\u00A0\u00A0456")).value.value.phoneNumber.value mustBe "0123 456"
    }

    "must treat a phone number of spaces only as not entered when a mobile number is given" in {
      val result = form.bind(Map("phoneNumber" -> "   ", "mobileNumber" -> "07700900123"))
      result.errors mustBe empty
      result.value.value mustBe ContactNumbers(None, Some("07700900123"))
    }

    "must treat a mobile number of spaces only as not entered when a phone number is given" in {
      val result = form.bind(Map("phoneNumber" -> "0123456", "mobileNumber" -> "\u00A0 \t"))
      result.errors mustBe empty
      result.value.value mustBe ContactNumbers(Some("0123456"), None)
    }

    "must return the form-level error when both numbers are spaces only" in {
      val result = form.bind(Map("phoneNumber" -> "   ", "mobileNumber" -> "  "))
      result.errors.map(_.key) mustBe Seq("")
    }

    "must still return the invalid error for other characters" in {
      form.bind(Map("phoneNumber" -> "0123-456")).errors.map(_.message) mustBe Seq(
        "changeContactNumbers.phoneNumber.error.invalid"
      )
    }

    "must apply the length check to the value after spaces are collapsed" in {
      val twentyChars = "0123456789 012345678"
      form
        .bind(Map("phoneNumber" -> s"   ${twentyChars.replace(" ", "     ")}   "))
        .value
        .value
        .phoneNumber
        .value mustBe twentyChars
    }
  }
}
