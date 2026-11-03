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

import controllers.actions.{AuthorisedAction, ClientListCheckAction, DataRequiredAction, DataRetrievalAction, HasClientGuard}
import models.BusinessDetails
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.GamblingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.agent.ManageMachineGamesDutyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageMachineGamesDutyController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  clientListCheck: ClientListCheckAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  hasClientGuard: HasClientGuard,
  gamblingService: GamblingService,
  val controllerComponents: MessagesControllerComponents,
  view: ManageMachineGamesDutyView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (authorise
      andThen clientListCheck
      andThen getData
      andThen requireData
      andThen hasClientGuard.currentClient).async { implicit request =>
      request.userAnswers
        .get(SelectedClientPage)
        .flatMap(AgentClientsPage.findInClientList(request.userAnswers, _)) match {
        case None         =>
          logger.warn("no selected client in userAnswers")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(client) =>
          (for {
            agentDetails  <- gamblingService.getAgentDetails
            returnSummary <- gamblingService.getReturnSummary(client.regNumber)
          } yield (agentDetails, returnSummary) match {
            case (Right(details), Right(summary)) =>
              Ok(
                view(
                  agentName = BusinessDetails.fromAgentDetails(details).businessName.getOrElse(""),
                  clientName = client.clientName.getOrElse(""),
                  regNumber = client.regNumber,
                  overdueReturns = summary.returnsOverdue,
                  // TODO: placeholder until the statement balance is available from the backend
                  balanceDate = "27 January 2025",
                  balance = BigDecimal("700.19"),
                  balanceType = "debit"
                )
              )
            case (agent, summary)                 =>
              logger.error(s"failed to load agent landing data: agentDetails=$agent returnSummary=$summary")
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
          }).recover { case e =>
            logger.error("failed to load manage your machine games duty", e)
            Redirect(controllers.routes.SystemErrorController.onPageLoad())
          }
      }
    }
}
