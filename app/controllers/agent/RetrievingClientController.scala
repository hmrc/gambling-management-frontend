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

package controllers.agent

import controllers.actions.AuthorisedAction
import models.agent.ClientListStatus
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.GamblingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.agent.RetrievingClientView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RetrievingClientController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  gamblingService: GamblingService,
  view: RetrievingClientView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val MaxRetries               = 8
  private val RefreshIntervalInSeconds = 15

  def onPageLoad: Action[AnyContent] = authorise { implicit request =>
    Ok(view())
      .withHeaders("Refresh" -> s"0; url=${routes.RetrievingClientController.start().url}")
  }

  def start: Action[AnyContent] = authorise.async { implicit request =>
    gamblingService.startClientListRetrieval
      .map {
        case ClientListStatus.Succeeded  => Redirect(routes.ClientListSearchController.onPageLoad())
        case ClientListStatus.Failed     => Redirect(routes.FailedToRetrieveClientController.onPageLoad())
        case ClientListStatus.InProgress => refreshResult(nextRetry = 1)
        case _                           => Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
      .recover { case _ =>
        Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
  }

  def poll(retryCount: Int = 0): Action[AnyContent] = authorise.async { implicit request =>
    val nextRetry = retryCount + 1

    if (nextRetry > MaxRetries) {
      Future.successful(Redirect(routes.FailedToRetrieveClientController.onPageLoad()))
    } else {
      gamblingService.getClientListStatus
        .map {
          case ClientListStatus.Succeeded  => Redirect(routes.ClientListSearchController.onPageLoad())
          case ClientListStatus.Failed     => Redirect(routes.FailedToRetrieveClientController.onPageLoad())
          case ClientListStatus.InProgress => refreshResult(nextRetry)
          case _                           => Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
        .recover { case _ =>
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }
  }

  private def refreshResult(nextRetry: Int)(implicit request: Request[_]): Result = {
    val refreshUrl = routes.RetrievingClientController.poll(retryCount = nextRetry).url

    Ok(view())
      .withHeaders("Refresh" -> s"$RefreshIntervalInSeconds; url=$refreshUrl")
  }
}
