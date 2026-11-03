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

import controllers.actions.{AgentOnlyAction, ClientListCheckAction, DataRetrievalAction}
import models.{BusinessDetails, GamblingError, UserAnswers}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{GamblingService, ManageService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.agent.ManageYourDetailsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ManageYourDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  agentOnly: AgentOnlyAction,
  clientListCheck: ClientListCheckAction,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  gamblingService: GamblingService,
  manageService: ManageService,
  view: ManageYourDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (agentOnly andThen clientListCheck andThen getData).async { implicit request =>
    val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

    (for {
      (clients, _) <- manageService.resolveAndStoreAgentClients(userAnswers)
      agentDetails <- gamblingService.getAgentDetails
    } yield agentDetails match {
      case Left(GamblingError.NotFound) =>
        logger.info(s"Agent details were not found for ${request.mgdRegNum}")
        Redirect(controllers.routes.PageNotFoundController.onPageLoad())
      case Left(_)                      => Redirect(controllers.routes.SystemErrorController.onPageLoad())
      case Right(details)               =>
        Ok(view(BusinessDetails.fromAgentDetails(details).businessName.getOrElse(""), clients.isEmpty))
    }).recover { case e =>
      logger.error("failed to load manage your details", e)
      Redirect(controllers.routes.SystemErrorController.onPageLoad())
    }
  }
}
