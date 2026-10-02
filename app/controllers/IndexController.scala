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
import models.{ReturnSummaryError, UserAnswers}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.IndexView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndexController @Inject() (
  authorise: AuthorisedAction,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  view: IndexView,
  gamblingService: GamblingService
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport:

  def onPageLoad(): Action[AnyContent] = authorise.async { implicit request =>

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val sessionKey = if request.isAgent then request.userId else request.mgdRegNum

    initialiseUserAnswers(sessionKey).flatMap { _ =>
      loadReturnSummary(request.mgdRegNum)
    }
  }

  private def initialiseUserAnswers(sessionKey: String): Future[UserAnswers] =
    sessionRepository.get(sessionKey).flatMap {
      case Some(userAnswers) => Future.successful(userAnswers)
      case None              =>
        val userAnswers = UserAnswers(sessionKey)
        sessionRepository.set(userAnswers).map(_ => userAnswers)
    }

  private def loadReturnSummary(mgdRegNumber: String)(using HeaderCarrier, play.api.mvc.Request[?]) =
    gamblingService
      .getReturnSummary(mgdRegNumber)
      .map {
        case Right(returnSummary) =>
          Ok(view(returnSummary))

        case Left(ReturnSummaryError.NotFound) =>
          NotFound("Return summary not found")

        case Left(ReturnSummaryError.UpstreamError) =>
          BadGateway("Upstream service error")

        case Left(ReturnSummaryError.UnexpectedError) =>
          InternalServerError("Unexpected error")
      }
