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
import forms.businessdetails.ChangeFaxNumberFormProvider
import models.{NormalMode, UserAnswers}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.businessdetails.ChangeFaxNumberPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.businessdetails.ChangeFaxNumberView

import scala.concurrent.Future

class ChangeBusinessContactControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val changeBusinessContactRoute = routes.ChangeBusinessContactController.onPageLoad().url

  "ChangeBusinessContactController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeBusinessContactRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1").text() mustEqual
          "Change your contact details"

        doc.select(".govuk-button").text() mustEqual
          "Submit"
      }
    }

    "must redirect when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, changeBusinessContactRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must redirect an organisation to the access denied page (agent-only page)" in {

      val application = organisationDeniedApplicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeBusinessContactRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.AccessDeniedController.onPageLoad().url
      }
    }
  }
}
