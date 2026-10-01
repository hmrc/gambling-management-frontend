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
import models.UserAnswers
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.businessdetails.ChangeFaxNumberPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository

import scala.concurrent.Future

class RemoveFaxNumberControllerSpec extends SpecBase with MockitoSugar {

  private val faxNumber = "087678886772"

  private val userAnswers =
    UserAnswers(userAnswersId)
      .set(ChangeFaxNumberPage, faxNumber)
      .success
      .value

  lazy val removeFaxNumberRoute =
    routes.RemoveFaxNumberController.onPageLoad().url

  "RemoveFaxNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request =
          FakeRequest(GET, removeFaxNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1").text() must include(faxNumber)

        doc.select("input[type=radio]").size() mustBe 2

        doc.select(".govuk-button").text() mustEqual
          "Continue"
      }
    }

    "must remove the fax number and redirect when Yes is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, removeFaxNumberRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.IndexController.onPageLoad().url

        val expectedAnswers =
          userAnswers
            .remove(ChangeFaxNumberPage)
            .success
            .value

        verify(mockSessionRepository).set(expectedAnswers)
      }
    }

    "must not remove the fax number when No is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, removeFaxNumberRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.IndexController.onPageLoad().url

        verify(mockSessionRepository, never()).set(any())
      }
    }

    "must return a Bad Request and errors when no option is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request =
          FakeRequest(POST, removeFaxNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1").text() must include(faxNumber)

        doc.select(".govuk-error-summary").size() mustBe 1

        doc.select(".govuk-error-message").text() must not be empty
      }
    }

    "must redirect to Index for a GET if the fax number is not found" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(GET, removeFaxNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.IndexController.onPageLoad().url
      }
    }

    "must redirect to Index for a POST if the fax number is not found" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(POST, removeFaxNumberRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.IndexController.onPageLoad().url
      }
    }
  }
}