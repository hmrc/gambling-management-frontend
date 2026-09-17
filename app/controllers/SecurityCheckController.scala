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
import models.agent.{ClientListCheckReturnTarget, ClientListStatus}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
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
  val controllerComponents: MessagesControllerComponents,
  view: SecurityCheckView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val MaxRetries               = 2
  private val RefreshIntervalInSeconds = 15

  def onPageLoad: Action[AnyContent] = authorise { implicit request =>
    Ok(view())
  }

  def onClientListCheck(returnTo: String, instanceId: Option[String]): Action[AnyContent] =
    authorise { implicit request =>
      returnCall(returnTo, instanceId) match {
        case Some(_) => refreshResult(returnTo, instanceId, retryCount = 0)
        case None    =>
          logger.warn(s"[SecurityCheckController] Invalid client list return target=$returnTo")
          systemError
      }
    }

  def pollClientListCheck(returnTo: String, instanceId: Option[String], retryCount: Int = 0): Action[AnyContent] =
    authorise.async { implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      returnCall(returnTo, instanceId) match {
        case None                 =>
          Future.successful(systemError)
        case Some(successfulCall) =>
          val nextRetry = retryCount + 1
          if nextRetry > MaxRetries then Future.successful(systemError)
          else
            gamblingService.getClientListStatus
              .map {
                case ClientListStatus.Succeeded                                  =>
                  Redirect(successfulCall)
                case ClientListStatus.InProgress                                 =>
                  refreshResult(returnTo, instanceId, nextRetry)
                case ClientListStatus.Failed | ClientListStatus.InitiateDownload =>
                  systemError
              }
              .recover { case NonFatal(e) =>
                logger.error("[SecurityCheckController] Client list polling failed", e)
                systemError
              }
      }
    }

  private def refreshResult(returnTo: String, instanceId: Option[String], retryCount: Int)(implicit
    request: Request[?]
  ): Result = {
    val refreshUrl = routes.SecurityCheckController.pollClientListCheck(returnTo, instanceId, retryCount).url
    Ok(view()).withHeaders("Refresh" -> s"$RefreshIntervalInSeconds; url=$refreshUrl")
  }

  private def returnCall(returnTo: String, instanceId: Option[String]): Option[Call] =
    returnTo match {
      case ClientListCheckReturnTarget.AgentLanding.key          =>
        instanceId.map(controllers.agent.routes.AgentLandingController.onPageLoad)
      case ClientListCheckReturnTarget.ClientList.key            =>
        Some(controllers.agent.routes.ClientListSearchController.onPageLoad())
      case ClientListCheckReturnTarget.ManageClientDetails.key   =>
        Some(controllers.clientdetails.routes.ManageClientDetailsController.onPageLoad())
      case ClientListCheckReturnTarget.ChangeClientReference.key =>
        instanceId.map(controllers.clientdetails.routes.ChangeClientReferenceController.onPageLoad)
      case ClientListCheckReturnTarget.RemoveClient.key          =>
        instanceId.map(controllers.clientdetails.routes.RemoveClientYesNoController.onPageLoad)
      case _                                                     =>
        None
    }

  private def systemError: Result =
    Redirect(controllers.routes.SystemErrorController.onPageLoad())
}
