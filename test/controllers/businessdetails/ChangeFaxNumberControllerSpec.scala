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
import forms.businessdetails.{ChangeEmailAddressFormProvider, ChangeFaxNumberFormProvider}
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.businessdetails.ChangeEmailAddressPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.businessdetails.{ChangeEmailAddressView, ChangeFaxNumberView}
import org.jsoup.Jsoup
import pages.ChangeFaxNumberPage

import scala.concurrent.Future

class ChangeFaxNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new ChangeFaxNumberFormProvider()
  val form = formProvider()

  lazy val changeFaxNumberRoute = routes.ChangeFaxNumberController.onPageLoad().url

  "ChangeFaxNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeFaxNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1").text() mustEqual
          "What is the fax number for this business?"

        doc.select("#value").`val`() mustEqual ""

        doc.select(".govuk-button").text() mustEqual
          "Continue"
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(ChangeFaxNumberPage, "01234567890")
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeFaxNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1").text() mustEqual
          "What is the fax number for this business?"

        doc.select("#value").`val`() mustEqual
          "01234567890"

        doc.select(".govuk-button").text() mustEqual
          "Continue"
      }
    }

    "must redirect when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, changeFaxNumberRoute)
            .withFormUrlEncodedBody(
              ("value", "01234567890")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeFaxNumberRoute)
            .withFormUrlEncodedBody(
              ("value", "")
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1").text() mustEqual
          "What is the fax number for this business?"

        doc.select(".govuk-error-summary").size() mustBe 1

        doc.select(".govuk-error-message").text() must not be empty
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, changeFaxNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, changeFaxNumberRoute)
            .withFormUrlEncodedBody(
              ("value", "01234567890")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
