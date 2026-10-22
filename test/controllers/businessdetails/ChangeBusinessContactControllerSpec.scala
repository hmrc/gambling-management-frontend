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
import connectors.GamblingConnector
import models.businessdetails.{BusinessContactDetailSubmission, ChrisSubmissionResponse}
import models.{BusinessDetails, UserAnswers}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.BusinessDetailsPage
import pages.businessdetails.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ChangeBusinessContactControllerSpec extends SpecBase with MockitoSugar {

  lazy val changeBusinessContactRoute = routes.ChangeBusinessContactController.onPageLoad().url

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

    "must clear the submitted contact details, keep other answers and redirect when submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val mockGamblingConnector = mock[GamblingConnector]

      when(
        mockGamblingConnector.updateBusinessDetails(
          any[BusinessContactDetailSubmission]
        )(using any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          ChrisSubmissionResponse(
            acknowledgementReference = "ABC123",
            submissionTimestamp = "2026-10-12T12:00:00"
          )
        )
      )

      val userAnswers =
        emptyUserAnswers
          .set(ChangeFaxNumberPage, "01881123456")
          .success
          .value
          .set(ChangeEmailAddressPage, "someone@someone.com")
          .success
          .value
          .set(BusinessDetailsPage, businessDetails)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[GamblingConnector].toInstance(mockGamblingConnector)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, changeBusinessContactRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.businessdetails.routes.ChangeBusinessContactConfirmationController
          .onPageLoad()
          .url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())

        val saved = captor.getValue
        saved.get(ChangeFaxNumberPage) mustBe None
        saved.get(RemoveFaxNumberPage) mustBe None
        saved.get(ChangeEmailAddressPage) mustBe None
        saved.get(RemoveEmailAddressPage) mustBe None
        saved.get(ChangeContactNumbersPage) mustBe None
        saved.get(BusinessDetailsPage) mustBe Some(businessDetails)
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
