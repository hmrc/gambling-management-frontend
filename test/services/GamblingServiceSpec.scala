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

package services

import connectors.GamblingConnector
import models.{MgdCertificate, ReturnSummary, ReturnSummaryError}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class GamblingServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  private val mgdRegNumber  = "XWM00000001762"
  private val mockConnector = mock[GamblingConnector]

  private val service = new GamblingService(mockConnector)

  private val certificate =
    MgdCertificate(
      mgdRegNumber = mgdRegNumber,
      registrationDate = None,
      individualName = None,
      businessName = None,
      tradingName = None,
      repMemName = None,
      busAddrLine1 = None,
      busAddrLine2 = None,
      busAddrLine3 = None,
      busAddrLine4 = None,
      busPostcode = None,
      busCountry = None,
      busAdi = None,
      repMemLine1 = None,
      repMemLine2 = None,
      repMemLine3 = None,
      repMemLine4 = None,
      repMemPostcode = None,
      repMemAdi = None,
      typeOfBusiness = None,
      businessTradeClass = None,
      noOfPartners = None,
      groupReg = "N",
      noOfGroupMems = None,
      dateCertIssued = None,
      partMembers = Seq.empty,
      groupMembers = Seq.empty,
      returnPeriodEndDates = Seq.empty
    )

  "getReturnSummary" should {

    "return success" in {
      val summary = mock[ReturnSummary]

      when(
        mockConnector.getReturnSummary(mgdRegNumber)
      ).thenReturn(Future.successful(Right(summary)))

      service.getReturnSummary(mgdRegNumber).map { result =>
        result mustBe Right(summary)
      }
    }

    "return not found" in {

      when(
        mockConnector.getReturnSummary(mgdRegNumber)
      ).thenReturn(Future.successful(Left(ReturnSummaryError.NotFound)))

      service.getReturnSummary(mgdRegNumber).map { result =>
        result mustBe Left(ReturnSummaryError.NotFound)
      }
    }
  }

  "MgdCertificateService#retrieveCertificate" should {

    "fetch from connector" in {

      when(mockConnector.getCertificate(mgdRegNumber))
        .thenReturn(Future.successful(certificate))

      val result = service.retrieveCertificate(mgdRegNumber).futureValue

      result mustBe certificate
    }

    "fail when connector fails" in {

      doReturn(Future.failed(new RuntimeException("backend failure")))
        .when(mockConnector)
        .getCertificate(mgdRegNumber)

      val ex = service.retrieveCertificate(mgdRegNumber).failed.futureValue

      ex.getMessage mustBe "backend failure"
    }
  }

}
