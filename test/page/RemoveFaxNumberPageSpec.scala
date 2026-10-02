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
import pages.businessdetails.ChangeFaxNumberPage
import pages.businessdetails.RemoveFaxNumberPage

class RemoveFaxNumberPageSpec extends SpecBase {

  private val faxNumber = "087678886772"

  "RemoveFaxNumberPage" - {

    "must remove ChangeFaxNumberPage when the answer is true" in {
      val userAnswers = emptyUserAnswers.set(ChangeFaxNumberPage, faxNumber).success.value
      val result      = RemoveFaxNumberPage.cleanup(Some(true), userAnswers).success.value

      result.get(ChangeFaxNumberPage) mustBe None
    }

    "must keep ChangeFaxNumberPage when the answer is false" in {
      val userAnswers = emptyUserAnswers.set(ChangeFaxNumberPage, faxNumber).success.value
      val result      = RemoveFaxNumberPage.cleanup(Some(false), userAnswers).success.value

      result.get(ChangeFaxNumberPage) mustBe Some(faxNumber)
    }

    "must keep ChangeFaxNumberPage when the answer is not set" in {
      val userAnswers = emptyUserAnswers.set(ChangeFaxNumberPage, faxNumber).success.value
      val result      = RemoveFaxNumberPage.cleanup(None, userAnswers).success.value

      result.get(ChangeFaxNumberPage) mustBe Some(faxNumber)
    }
  }
}
