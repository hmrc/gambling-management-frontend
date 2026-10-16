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

package page

import base.SpecBase
import pages.businessdetails.ChangeEmailAddressPage
import pages.businessdetails.RemoveEmailAddressPage

class RemoveEmailAddressPageSpec extends SpecBase {

  private val emailAddress = "abc.def@gmail.com"

  "RemoveEmailAddressPage" - {

    "must remove ChangeEmailAddressPage when the answer is true" in {
      val userAnswers = emptyUserAnswers.set(ChangeEmailAddressPage, emailAddress).success.value
      val result      = RemoveEmailAddressPage.cleanup(Some(true), userAnswers).success.value

      result.get(ChangeEmailAddressPage) mustBe None
    }

    "must keep ChangeEmailAddressPage when the answer is false" in {
      val userAnswers = emptyUserAnswers.set(ChangeEmailAddressPage, emailAddress).success.value
      val result      = RemoveEmailAddressPage.cleanup(Some(false), userAnswers).success.value

      result.get(ChangeEmailAddressPage) mustBe Some(emailAddress)
    }

    "must keep ChangeEmailAddressPage when the answer is not set" in {
      val userAnswers = emptyUserAnswers.set(ChangeEmailAddressPage, emailAddress).success.value
      val result      = RemoveEmailAddressPage.cleanup(None, userAnswers).success.value

      result.get(ChangeEmailAddressPage) mustBe Some(emailAddress)
    }
  }
}
