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

import config.AppConfig
import controllers.actions.AuthorisedAction
import models.agent.ClientListStatus
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.SecurityCheckView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SecurityCheckController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  gamblingService: GamblingService,
  config: AppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: SecurityCheckView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val refreshIntervalInSeconds = config.clientListPollIntervalSeconds
  private val maxRetries               = math.ceil(config.clientListPollMaxWaitSeconds.toDouble / refreshIntervalInSeconds).toInt

  def onPageLoad: Action[AnyContent] = authorise { implicit request =>
    Ok(view())
  }

  def onClientListCheck(continueUrl: RedirectUrl): Action[AnyContent] =
    authorise { implicit request =>
      safeUrl(continueUrl) match {
        case Some(_) => refreshResult(continueUrl, retryCount = 0)
        case None    =>
          logger.warn("Invalid client list return url")
          systemError
      }
    }

  def pollClientListCheck(continueUrl: RedirectUrl, retryCount: Int = 0): Action[AnyContent] =
    authorise.async { implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      safeUrl(continueUrl) match {
        case None            =>
          Future.successful(systemError)
        case Some(returnUrl) =>
          val nextRetry = retryCount + 1
          if nextRetry > maxRetries then Future.successful(systemError)
          else
            gamblingService.getClientListStatus
              .map {
                case ClientListStatus.Succeeded                                      =>
                  Redirect(returnUrl)
                case ClientListStatus.InProgress | ClientListStatus.InitiateDownload =>
                  refreshResult(continueUrl, nextRetry)
                case ClientListStatus.Failed                                         =>
                  systemError
              }
              .recover { case NonFatal(e) =>
                logger.error("Client list polling failed", e)
                systemError
              }
      }
    }

  private def refreshResult(continueUrl: RedirectUrl, retryCount: Int)(implicit request: Request[?]): Result = {
    val refreshUrl = routes.SecurityCheckController.pollClientListCheck(continueUrl, retryCount).url
    Ok(view()).withHeaders("Refresh" -> s"$refreshIntervalInSeconds; url=$refreshUrl")
  }

  private def safeUrl(continueUrl: RedirectUrl): Option[String] =
    continueUrl.getEither(OnlyRelative).toOption.map(_.url)

  private def systemError: Result =
    Redirect(controllers.routes.SystemErrorController.onPageLoad())
}
