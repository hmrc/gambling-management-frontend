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
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.businessdetails.{ChangeFaxNumberPage, RemoveFaxNumberPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class RemoveFaxNumberControllerSpec extends SpecBase with MockitoSugar {

  private val faxNumber = "087678886772"

  private val userAnswers =
    UserAnswers(userAnswersId)
      .set(ChangeFaxNumberPage, faxNumber)
      .success
      .value

  private val onwardRoute = Call("GET", "/foo")

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
        doc.select(".govuk-button").text() mustEqual "Continue"
      }
    }

    "must remove the fax number, save the answer and redirect when Yes is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockNavigator         = mock[Navigator]

      val expectedAnswers =
        userAnswers
          .set(RemoveFaxNumberPage, true)
          .success
          .value

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      when(
        mockNavigator.nextPage(
          RemoveFaxNumberPage,
          NormalMode,
          expectedAnswers
        )
      ).thenReturn(onwardRoute)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[Navigator].toInstance(mockNavigator)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, removeFaxNumberRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        expectedAnswers.get(ChangeFaxNumberPage) mustBe None
        expectedAnswers.get(RemoveFaxNumberPage) mustBe Some(true)

        verify(mockSessionRepository).set(expectedAnswers)

        verify(mockNavigator).nextPage(
          RemoveFaxNumberPage,
          NormalMode,
          expectedAnswers
        )
      }
    }

    "must keep the fax number, save the answer and redirect when No is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockNavigator         = mock[Navigator]

      val expectedAnswers =
        userAnswers
          .set(RemoveFaxNumberPage, false)
          .success
          .value

      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))

      when(
        mockNavigator.nextPage(
          RemoveFaxNumberPage,
          NormalMode,
          expectedAnswers
        )
      ).thenReturn(onwardRoute)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[Navigator].toInstance(mockNavigator)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, removeFaxNumberRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        expectedAnswers.get(ChangeFaxNumberPage) mustBe Some(faxNumber)
        expectedAnswers.get(RemoveFaxNumberPage) mustBe Some(false)

        verify(mockSessionRepository).set(expectedAnswers)

        verify(mockNavigator).nextPage(
          RemoveFaxNumberPage,
          NormalMode,
          expectedAnswers
        )
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

        doc.select("h1").text()                   must include(faxNumber)
        doc.select(".govuk-error-summary").size() mustBe 1
        doc.select(".govuk-error-message").text() must not be empty
      }
    }

    "must redirect to Journey Recovery for a GET when the fax number is not found" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(GET, removeFaxNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when the fax number is not found" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(POST, removeFaxNumberRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect an organisation to the access denied page (agent-only page)" in {

      val application = organisationDeniedApplicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, removeFaxNumberRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.AccessDeniedController.onPageLoad().url
      }
    }
  }
}
