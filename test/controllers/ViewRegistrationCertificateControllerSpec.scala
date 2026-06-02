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
import models.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.DefaultActionBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.Html
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.registrationCertificate.ViewRegistrationCertificateView

import java.time.LocalDate
import scala.concurrent.Future

class ViewRegistrationCertificateControllerSpec extends SpecBase with MockitoSugar {

  private val certificate: MgdCertificate =
    MgdCertificate(
      mgdRegNumber = "MGD123",
      registrationDate = Some(LocalDate.parse("2026-01-01")),
      individualName = Some("John Doe"),
      businessName = Some("Test Business Ltd"),
      tradingName = None,
      repMemName = None,
      busAddrLine1 = Some("Line 1"),
      busAddrLine2 = Some("Line 2"),
      busAddrLine3 = None,
      busAddrLine4 = None,
      busPostcode = Some("AB1 2CD"),
      busCountry = None,
      busAdi = None,
      repMemLine1 = None,
      repMemLine2 = None,
      repMemLine3 = None,
      repMemLine4 = None,
      repMemPostcode = None,
      repMemAdi = None,
      typeOfBusiness = Some("Corporate Body"), // important: matches controller
      businessTradeClass = Some(1),
      noOfPartners = None,
      groupReg = "N",
      noOfGroupMems = None,
      dateCertIssued = Some(LocalDate.parse("2026-01-02")),
      partMembers = Seq.empty,
      groupMembers = Seq.empty,
      returnPeriodEndDates = Seq.empty
    )

  "ViewRegistrationCertificateController" - {

    "must return OK and render certificate view when service succeeds" in {

      val mockService = mock[GamblingService]

      when(
        mockService.retrieveCertificate(any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(certificate))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ViewRegistrationCertificateController.onPageLoad().url)
            .withSession("mgdRefNum" -> "MGD123")

        val result = route(application, request).value
        val view   = application.injector.instanceOf[ViewRegistrationCertificateView]

        val (displayName, displayLabelKey) =
          certificate.typeOfBusiness.map(_.trim.toLowerCase) match {
            case Some("sole proprietor") =>
              certificate.individualName.getOrElse("") ->
                "viewRegistrationCertificate.label.soleProprietor"

            case Some("unincorporated body") =>
              certificate.businessName.getOrElse("") ->
                "viewRegistrationCertificate.label.unincorporatedBody"

            case Some("corporate body") =>
              certificate.businessName.getOrElse("") ->
                "viewRegistrationCertificate.label.corporateBody"

            case Some("partnership") =>
              certificate.partMembers.headOption.flatMap(_.namesOfPartMems).getOrElse("") ->
                "viewRegistrationCertificate.label.partnership"

            case Some("limited liability partnership") =>
              certificate.businessName.getOrElse("") ->
                "viewRegistrationCertificate.label.limitedLiabilityPartnership"

            case _ =>
              certificate.businessName.getOrElse("") ->
                "viewRegistrationCertificate.label.default"
          }

        val formattedAddress =
          Seq(
            certificate.busAddrLine1,
            certificate.busAddrLine2,
            certificate.busAddrLine3,
            certificate.busAddrLine4,
            certificate.busPostcode
          ).flatten.filter(_.nonEmpty).mkString("<br>")

        status(result) mustBe OK

        status(result) mustBe OK

        def normalize(html: String): String =
          html
            .replaceAll(""" nonce="[^"]*"""", "")
            .replaceAll("""<script\s+>""", "<script>")
            .replaceAll(""""\s+>""", "\">")
            .replaceAll(""">\s+<""", "><")
            .replaceAll("""\s+""", " ")
            .trim

        val expected =
          view(
            certificate,
            displayName,
            displayLabelKey,
            formattedAddress
          )(request, messages(application)).toString

        normalize(contentAsString(result)) mustBe normalize(expected)
      }
    }

    "must use sole proprietor branch when typeOfBusiness is sole proprietor" in {

      val solePropCertificate = certificate.copy(
        typeOfBusiness = Some("sole proprietor"),
        individualName = Some("John Sole")
      )

      val mockService = mock[GamblingService]

      when(
        mockService.retrieveCertificate(any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(solePropCertificate))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ViewRegistrationCertificateController.onPageLoad().url)
            .withSession("mgdRefNum" -> "MGD123")

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) must include("Sole proprietor’s name")
        contentAsString(result) must include("John Sole")
      }
    }

    "must use partnership branch when typeOfBusiness is partnership" in {

      val partnershipCert = certificate.copy(
        typeOfBusiness = Some("partnership"),
        businessName = Some("Partner Business Ltd")
      )

      val mockService = mock[GamblingService]
      val mockView    = mock[ViewRegistrationCertificateView]

      when(
        mockService.retrieveCertificate(any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(partnershipCert))

      when(
        mockView.apply(
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      ).thenReturn(Html("success"))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[GamblingService].toInstance(mockService),
            bind[ViewRegistrationCertificateView].toInstance(mockView)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ViewRegistrationCertificateController.onPageLoad().url)
            .withSession("mgdRefNum" -> "MGD123")

        val result = route(application, request).value

        status(result) mustBe OK

        verify(mockView).apply(
          eqTo(partnershipCert),
          eqTo("Partner Business Ltd"),
          eqTo("viewRegistrationCertificate.label.partnership"),
          any()
        )(any(), any())
      }
    }

    "must use unincorporated body branch when typeOfBusiness is unincorporated body" in {

      val unincorpCert = certificate.copy(
        typeOfBusiness = Some("unincorporated body"),
        businessName = Some("Unincorporated Group Ltd")
      )

      val mockService = mock[GamblingService]

      when(
        mockService.retrieveCertificate(any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(unincorpCert))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ViewRegistrationCertificateController.onPageLoad().url)
            .withSession("mgdRefNum" -> "MGD123")

        val result = route(application, request).value

        status(result) mustBe OK

        contentAsString(result) must include("Unincorporated Group Ltd")
        contentAsString(result) must include("Unincorporated body")
      }
    }

    "must use limited liability partnership branch when typeOfBusiness is LLP" in {

      val llpCert = certificate.copy(
        typeOfBusiness = Some("limited liability partnership"),
        businessName = Some("LLP Business Ltd")
      )

      val mockService = mock[GamblingService]

      when(
        mockService.retrieveCertificate(any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(llpCert))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ViewRegistrationCertificateController.onPageLoad().url)
            .withSession("mgdRefNum" -> "MGD123")

        val result = route(application, request).value

        status(result) mustBe OK

        contentAsString(result) must include("LLP Business Ltd")
        contentAsString(result) must include("limited liability partnership")
      }
    }

    "must redirect to system error page when service fails" in {

      val mockService = mock[GamblingService]

      when(
        mockService.retrieveCertificate(any[String])(any[HeaderCarrier])
      ).thenReturn(Future.failed(new RuntimeException("boom")))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[GamblingService].toInstance(mockService))
          .build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ViewRegistrationCertificateController.onPageLoad().url)
            .withSession("mgdRefNum" -> "MGD123")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
