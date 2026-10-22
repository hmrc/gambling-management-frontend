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
import controllers.businessdetails.ConfirmBusinessNameController
import models.UserAnswers
import models.businessdetails.{BusinessNameSubmission, ChrisSubmissionResponse}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.businessdetails.BusinessNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ConfirmBusinessNameControllerSpec extends SpecBase with MockitoSugar {

  lazy val confirmBusinessNameRoute: String = routes.ConfirmBusinessNameController.onPageLoad().url
  lazy val businessNameRoute: String        = routes.BusinessNameController.onPageLoad().url

  "ConfirmBusinessName Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = UserAnswers(userAnswersId).set(BusinessNamePage, "Test Ltd").success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, confirmBusinessNameRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1").text() mustEqual "Confirm your business name"
        doc.select(".govuk-summary-list__value").text() mustEqual "Test Ltd"
        doc.select(".govuk-summary-list__actions a").attr("href") mustEqual businessNameRoute
        doc.select(".govuk-button").text() mustEqual "Submit"
      }
    }

    "must redirect to Business Name when no name has been entered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, confirmBusinessNameRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual businessNameRoute
      }
    }

    "must call the service to update the business name when submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val mockGamblingService = mock[GamblingService]
      when(
        mockGamblingService.updateBusinessName(any[BusinessNameSubmission])(using any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          ChrisSubmissionResponse(
            acknowledgementReference = "ABC123",
            submissionTimestamp = "2026-10-12T12:00:00"
          )
        )
      )

      val userAnswers = UserAnswers(userAnswersId).set(BusinessNamePage, "Test Ltd").success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[GamblingService].toInstance(mockGamblingService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, routes.ConfirmBusinessNameController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ChangeBusinessNameConfirmationController.onPageLoad().url

        val captor = ArgumentCaptor.forClass(classOf[BusinessNameSubmission])
        verify(mockGamblingService).updateBusinessName(captor.capture())(using any[HeaderCarrier])
        captor.getValue mustBe BusinessNameSubmission("Test Ltd", None)

        val captorUa = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captorUa.capture())
        captorUa.getValue.get(BusinessNamePage) mustBe None
      }
    }

    "must redirect to Business Name when submitting without a name" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.ConfirmBusinessNameController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual businessNameRoute
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, confirmBusinessNameRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, routes.ConfirmBusinessNameController.onSubmit().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect an organisation to the access denied page" in {

      val application = organisationDeniedApplicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, confirmBusinessNameRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.AccessDeniedController.onPageLoad().url
      }
    }
  }
}
