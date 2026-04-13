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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.http.Status

class IndexControllerSpec extends SpecBase {

  "IndexController" - {

    "must return OK for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val controller = application.injector.instanceOf[IndexController]

      val request = FakeRequest(GET, "/")

      val result = controller.onPageLoad()(request)

      status(result) mustBe Status.OK
    }

    "must return HTML" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val controller = application.injector.instanceOf[IndexController]

      val request = FakeRequest(GET, "/")

      val result = controller.onPageLoad()(request)

      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }
  }
}
