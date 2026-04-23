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
import models.{ReturnSummary, ReturnSummaryError}
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReturnSummaryServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  private val mgdRegNumber = "XWM00000001762"

  private val mockConnector = mock[GamblingConnector]

  private val service = new ReturnSummaryService(mockConnector)

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
}
