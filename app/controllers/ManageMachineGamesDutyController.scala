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

import controllers.actions.{AuthorisedAction, ClientListCheckAction, DataRetrievalAction, HasClientGuard}
import models.BusinessDetails
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import models.requests.OptionalDataRequest
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GamblingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ManageMachineGamesDutyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageMachineGamesDutyController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  clientListCheck: ClientListCheckAction,
  getData: DataRetrievalAction,
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
      andThen hasClientGuard.currentClientAuthorised
      andThen getData).async { implicit request =>
      if request.isAgent then agentPage else organisationPage
    }

  private def agentPage(using request: OptionalDataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .flatMap(answers =>
        answers.get(SelectedClientPage).flatMap(AgentClientsPage.findInClientList(answers, _))
      ) match {
      case None         =>
        logger.warn("no selected client in userAnswers")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(client) =>
        (for {
          agentDetails  <- gamblingService.getAgentDetails
          returnSummary <- gamblingService.getReturnSummary(client.regNumber)
        } yield (agentDetails, returnSummary) match {
          case (Right(details), Right(summary)) =>
            buildResult(
              isAgent = true,
              agentName =
                nameOrBlank(BusinessDetails.fromAgentDetails(details).businessName, "agent name", client.regNumber),
              clientName = nameOrBlank(client.clientName, "client name", client.regNumber),
              regNumber = client.regNumber,
              overdueReturns = summary.returnsOverdue
            )
          case (agent, summary)                 =>
            logger.error(
              s"failed to load manage your MGD data for regNumber=${client.regNumber}: agentDetails=$agent returnSummary=$summary"
            )
            Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }).recover(systemError(client.regNumber))
    }

  private def nameOrBlank(name: Option[String], label: String, regNumber: String): String =
    name.getOrElse {
      logger.warn(s"$label not found for regNumber=$regNumber")
      ""
    }

  private def organisationPage(using request: OptionalDataRequest[AnyContent]): Future[Result] =
    gamblingService
      .getReturnSummary(request.mgdRegNum)
      .map {
        case Right(summary) => buildResult(isAgent = false, "", "", request.mgdRegNum, summary.returnsOverdue)
        case Left(error)    =>
          logger.error(s"failed to load return summary for regNumber=${request.mgdRegNum}: $error")
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
      .recover(systemError(request.mgdRegNum))

  private def buildResult(
    isAgent: Boolean,
    agentName: String,
    clientName: String,
    regNumber: String,
    overdueReturns: Int
  )(using
    request: OptionalDataRequest[AnyContent]
  ): Result =
    Ok(
      view(
        isAgent = isAgent,
        agentName = agentName,
        clientName = clientName,
        regNumber = regNumber,
        overdueReturns = overdueReturns,
        // TODO: placeholder until the statement balance is available from the backend
        balanceDate = "27 January 2025",
        balance = BigDecimal("700.19"),
        balanceType = "debit"
      )
    )

  private def systemError(regNumber: String): PartialFunction[Throwable, Result] = { case e =>
    logger.error(s"failed to load manage your machine games duty for regNumber=$regNumber", e)
    Redirect(controllers.routes.SystemErrorController.onPageLoad())
  }
}
