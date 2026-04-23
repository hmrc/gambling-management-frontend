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

import controllers.actions.AuthorisedAction
import models.{ReturnSummary, ReturnSummaryError}
import models.requests.AuthorisedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.*
import play.api.test.*
import play.api.test.Helpers.*
import services.ReturnSummaryService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import views.html.IndexView

import scala.concurrent.{ExecutionContext, Future}

class IndexControllerSpec extends AsyncWordSpec with Matchers with MockitoSugar with ScalaFutures {

  given ExecutionContext = ExecutionContext.global

  private val stubView = new IndexView(null) {
    override def apply(summary: ReturnSummary)(implicit
      request: Request[_],
      messages: Messages
    ) =
      play.twirl.api.Html("ok")
  }

  private val mockAuthorisedAction = mock[AuthorisedAction]

  when(
    mockAuthorisedAction.async(
      any[AuthorisedRequest[AnyContent] => Future[Result]]
    )
  ).thenAnswer { invocation =>

    val block =
      invocation.getArgument[AuthorisedRequest[AnyContent] => Future[Result]](0)

    actionBuilder.async { implicit request =>
      val authorisedRequest =
        AuthorisedRequest(
          request,
          AffinityGroup.Individual,
          "test-mgd-ref"
        )

      block(authorisedRequest)
    }
  }

  private val mcc           = stubMessagesControllerComponents()
  private val actionBuilder = DefaultActionBuilder(stubBodyParser())
  private val fakeRequest   = FakeRequest(GET, "/")

  "IndexController.onPageLoad" should {

    "return 200 OK when service succeeds" in {

      val summary = mock[ReturnSummary]

      val stubService = new ReturnSummaryService(null) {
        override def getReturnSummary(
          mgdRegNumber: String
        )(using hc: HeaderCarrier): Future[Either[ReturnSummaryError, ReturnSummary]] =
          Future.successful(Right(summary))
      }

      val controller =
        new IndexController(
          mockAuthorisedAction,
          mcc,
          stubView,
          stubService
        )

      controller.onPageLoad()(fakeRequest).map { result =>
        result.header.status mustBe OK
      }
    }

    "return 404 when service returns NotFound" in {

      val stubService = new ReturnSummaryService(null) {
        override def getReturnSummary(
          mgdRegNumber: String
        )(using hc: HeaderCarrier): Future[Either[ReturnSummaryError, ReturnSummary]] =
          Future.successful(Left(ReturnSummaryError.NotFound))
      }

      val controller =
        new IndexController(
          mockAuthorisedAction,
          mcc,
          stubView,
          stubService
        )

      controller.onPageLoad()(fakeRequest).map { result =>
        result.header.status mustBe NOT_FOUND
      }
    }
  }
}
