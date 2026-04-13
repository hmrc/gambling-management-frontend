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

package controllers

import base.SpecBase
import config.AppConfig
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.PageNotFoundView

class PageNotFoundControllerSpec extends SpecBase {

  "PageNotFound Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.PageNotFoundController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Page not found")
        contentAsString(result) must include("HMRC Online Services Helpdesk")
        contentAsString(result) must include("If you typed the web address, check it is correct.")
        contentAsString(result) must include(
          "If the web address is correct or you selected a link or button, contact the"
        )
      }
    }
  }
}
