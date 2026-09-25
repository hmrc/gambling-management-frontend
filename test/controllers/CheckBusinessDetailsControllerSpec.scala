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
import models.BusinessDetails
import org.jsoup.Jsoup
import pages.BusinessDetailsPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class CheckBusinessDetailsControllerSpec extends SpecBase {

  private val businessDetails = BusinessDetails(
    businessName = Some("Agent 1"),
    addressLine1 = Some("123 Business road"),
    addressLine2 = Some("Business"),
    addressLine3 = Some("London"),
    phoneNumber = Some("0191 202 2500"),
    mobileNumber = Some("07890 123 456"),
    faxNumber = Some("0800 202 2500"),
    emailAddress = Some("sarah.phillips@example.com")
  )

  "Check Business Details Controller" - {

    "must return OK and render the values already stored in the user's answers" in {
      val userAnswers = emptyUserAnswers.set(BusinessDetailsPage, businessDetails).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckBusinessDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.text() must include("Agent 1")
        doc.text() must include("0191 202 2500")
        doc.text() must include("sarah.phillips@example.com")
        doc.select(".govuk-summary-list").size() mustEqual 1
        doc.select(".govuk-summary-list__row").size() mustEqual 3
        doc.select(".govuk-summary-list__key").size() mustEqual 3
        doc.select(".govuk-summary-list__value.govuk-\\!-text-align-left").size() mustEqual 3
        doc.select(".govuk-summary-list__actions").size() mustEqual 3
      }
    }

    "must return OK with default values when nothing is held in the user's answers" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckBusinessDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Agent1")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckBusinessDetailsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
