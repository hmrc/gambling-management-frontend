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

package controllers.businessdetails

import base.SpecBase
import models.businessdetails.ChrisSubmissionResponse
import pages.businessdetails.BusinessContactDetailConfirmationPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.businessdetails.UpdateBusinessContactConfirmationView

import scala.jdk.CollectionConverters.*
import org.jsoup.Jsoup

class ChangeBusinessContactConfirmationControllerSpec extends SpecBase {

  private val response = ChrisSubmissionResponse(
    acknowledgementReference = "ABC123",
    submissionTimestamp = "2026-10-12T12:00:00"
  )

  private val userAnswers =
    emptyUserAnswers
      .set(BusinessContactDetailConfirmationPage, response)
      .success
      .value

  "UpdateBusinessContactConfirmationController" - {

    "must return OK and the correct content for a GET" in {

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.ChangeBusinessContactConfirmationController
              .onPageLoad()
              .url
          )

        val result = route(application, request).value

        status(result) mustEqual OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1").text() mustEqual
          "Changes submitted"

        doc.select(".govuk-panel__body").text() must include(
          "Your reference number is"
        )

        doc.select(".govuk-panel__body strong").text() mustEqual
          "ABC123"

        doc.select("main h2.govuk-heading-m").text() mustEqual
          "What happens next"

        doc.body().text() must include(
          "We are reviewing your changes and will notify you if there are any issues."
        )

        doc.body().text() must include(
          "It may take up to 48 hours for HMRC to approve your changes."
        )

        val returnLink =
          doc
            .select("a.govuk-link")
            .asScala
            .find(_.text() == "Return to check your business details")
            .value

        returnLink.attr("href") mustEqual
          routes.ChangeBusinessContactController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when response is not present" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(
            GET,
            routes.ChangeBusinessContactConfirmationController.onPageLoad().url
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
