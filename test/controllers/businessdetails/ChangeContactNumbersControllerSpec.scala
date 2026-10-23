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
import com.google.inject.name.Names
import controllers.actions.*
import forms.businessdetails.ChangeContactNumbersFormProvider
import models.agent.ContactNumbers
import models.{BusinessDetails, CheckMode, NormalMode, UserAnswers}
import navigation.Navigator
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.businessdetails.{ChangeContactNumbersPage, ChangeEmailAddressPage, ChangeFaxNumberPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.businessdetails.ChangeContactNumbersView

import scala.concurrent.Future

class ChangeContactNumbersControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/manage-gambling-tax/agent/business-details/change-business-contact-details")

  val formProvider               = new ChangeContactNumbersFormProvider()
  val form: Form[ContactNumbers] = formProvider()

  val validPhoneNumber                         = "01632 960 001"
  val validMobileNumber                        = "07700 900 982"
  val validContactNumberPhone: ContactNumbers  = ContactNumbers(Some(validPhoneNumber), None)
  val validContactNumberMobile: ContactNumbers = ContactNumbers(None, Some(validMobileNumber))
  val validContactNumbersBoth: ContactNumbers  = ContactNumbers(Some(validPhoneNumber), Some(validMobileNumber))

  val baseAnswers: UserAnswers =
    UserAnswers(userAnswersId).set(ChangeEmailAddressPage, "test@email.co.uk").success.value

  lazy val phoneNumberRoute: String       = routes.ChangeContactNumbersController.onPageLoad().url
  lazy val phoneNumberSubmitRoute: String = routes.ChangeContactNumbersController.onSubmit().url

  "ChangeContactNumbersController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = addCSRFToken(FakeRequest(GET, phoneNumberRoute))

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChangeContactNumbersView]

        status(result) mustEqual OK
        normalize(contentAsString(result)) mustEqual normalize(
          view(form, NormalMode)(request, messages(application)).toString
        )
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered with a phoneNumber" in {

      val userAnswers = baseAnswers.set(ChangeContactNumbersPage, validContactNumberPhone).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = addCSRFToken(FakeRequest(GET, phoneNumberRoute))

        val view = application.injector.instanceOf[ChangeContactNumbersView]

        val result = route(application, request).value

        status(result) mustEqual OK
        normalize(contentAsString(result)) mustEqual normalize(
          view(form.fill(validContactNumberPhone), NormalMode)(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must populate the view correctly when ChangeContactNumbersPage has been pre-populated" in {

      val userAnswers =
        baseAnswers
          .set(ChangeContactNumbersPage, validContactNumbersBoth)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request =
          addCSRFToken(FakeRequest(GET, phoneNumberRoute))

        val view =
          application.injector.instanceOf[ChangeContactNumbersView]

        val result =
          route(application, request).value

        status(result) mustEqual OK

        normalize(contentAsString(result)) mustEqual normalize(
          view(
            form.fill(validContactNumbersBoth),
            NormalMode
          )(request, messages(application)).toString
        )
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered with a mobileNumber" in {

      val userAnswers = baseAnswers.set(ChangeContactNumbersPage, validContactNumberMobile).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = addCSRFToken(FakeRequest(GET, phoneNumberRoute))

        val view = application.injector.instanceOf[ChangeContactNumbersView]

        val result = route(application, request).value

        status(result) mustEqual OK
        normalize(contentAsString(result)) mustEqual normalize(
          view(form.fill(validContactNumberMobile), NormalMode)(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered with both numbers" in {

      val userAnswers = baseAnswers.set(ChangeContactNumbersPage, validContactNumbersBoth).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = addCSRFToken(FakeRequest(GET, phoneNumberRoute))

        val view = application.injector.instanceOf[ChangeContactNumbersView]

        val result = route(application, request).value

        status(result) mustEqual OK
        normalize(contentAsString(result)) mustEqual normalize(
          view(form.fill(validContactNumbersBoth), NormalMode)(
            request,
            messages(application)
          ).toString
        )
      }
    }

    "must redirect to the next page and update session when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("phoneNumber", validPhoneNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())

        val saved = captor.getValue
        saved.get(ChangeFaxNumberPage) mustBe None
        saved.get(ChangeContactNumbersPage) mustBe Some(validContactNumberPhone)
        saved.get(ChangeEmailAddressPage) mustBe Some("test@email.co.uk")
      }
    }

    "must redirect to the next page when only a mobile number is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("mobileNumber", validMobileNumber))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must show an error when neither number has been entered" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("phoneNumber", ""), ("mobileNumber", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val content = contentAsString(result)
        content must include("href=\"#phoneNumber\"") // error line is linked to the first phone field
        content must include("id=\"phoneNumber-error\"")
        content must include(messages(application)("changeContactNumbers.error.phoneRequired"))
      }
    }

    "must show an error when both numbers have invalid characters" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("phoneNumber", "abcd"), ("mobileNumber", "00!£$"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val content = contentAsString(result)
        content must include("href=\"#phoneNumber\"")
        content must include("id=\"phoneNumber-error\"")
        content must include(messages(application)("changeContactNumbers.phoneNumber.error.invalid"))

        content must include("href=\"#mobileNumber\"")
        content must include("id=\"mobileNumber-error\"")
        content must include(messages(application)("changeContactNumbers.mobileNumber.error.invalid"))
      }
    }

    "must show an error both numbers have invalid length" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("phoneNumber", "123456789012345678909"), ("mobileNumber", "123456789012345678901"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val content = contentAsString(result)
        content must include("href=\"#phoneNumber\"")
        content must include("id=\"phoneNumber-error\"")
        content must include(messages(application)("changeContactNumbers.phoneNumber.error.length"))

        content must include("href=\"#mobileNumber\"")
        content must include("id=\"mobileNumber-error\"")
        content must include(messages(application)("changeContactNumbers.mobileNumber.error.length"))
      }
    }

    "must show an error when mobileNumber has invalid characters" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("phoneNumber", ""), ("mobileNumber", "00!£$"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val content = contentAsString(result)
        content must include("href=\"#mobileNumber\"")
        content must include("id=\"mobileNumber-error\"")
        content must include(messages(application)("changeContactNumbers.mobileNumber.error.invalid"))
      }
    }

    "must show an error when mobileNumber has invalid length" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, phoneNumberSubmitRoute)
            .withFormUrlEncodedBody(("phoneNumber", ""), ("mobileNumber", "123456789012345678901"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val content = contentAsString(result)
        content must include("href=\"#mobileNumber\"")
        content must include("id=\"mobileNumber-error\"")
        content must include(messages(application)("changeContactNumbers.mobileNumber.error.length"))
      }
    }

  }
}
