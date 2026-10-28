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

import controllers.actions.{AuthorisedAction, ClientListCheckAction, DataRequiredAction, DataRetrievalAction}
import models.UserAnswers
import pages.AgentClientsPage
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ManageService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.agent.ClientListViewModel
import views.html.agent.ClientListSearchView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientListSearchController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  clientListCheck: ClientListCheckAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  manageService: ManageService,
  val controllerComponents: MessagesControllerComponents,
  view: ClientListSearchView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen clientListCheck andThen getData).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val userAnswers                = request.userAnswers.getOrElse(UserAnswers(request.userId))
      val searchQuery                = request.getQueryString("search").getOrElse("")

      manageService
        .resolveAndStoreAgentClients(userAnswers)
        .map {
          case (Nil, _)     =>
            Redirect(controllers.agent.routes.NoAuthorisedClientsController.onPageLoad())
          case (clients, _) =>
            val allClients      = ClientListViewModel.fromAgentClients(clients)
            val filteredClients = ClientListViewModel.filterByName(searchQuery, allClients)
            Ok(view(filteredClients, searchQuery))
        }
        .recover { case e =>
          logger.error("failed to resolve and store agent clients", e)
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }

  def downloadClientList(): Action[AnyContent] =
    (authorise andThen clientListCheck andThen getData andThen requireData).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      manageService
        .resolveAndStoreAgentClients(request.userAnswers)
        .map {
          case (Nil, _)        =>
            Redirect(controllers.agent.routes.NoAuthorisedClientsController.onPageLoad())
          case (clientList, _) =>
            val clientsToDownload = ClientListViewModel.fromAgentClients(clientList)

            implicit val msgs: Messages = messagesApi.preferred(request)
            val header                  = Seq(
              msgs("clientListSearch.download.th.clientName"),
              msgs("clientListSearch.download.th.regNumber"),
              msgs("clientListSearch.download.th.clientReference")
            ).mkString(",")

            val rows: Seq[String] = clientsToDownload.map { c =>
              val name      = csvEscape(c.clientName)
              val regNumber = csvEscape(c.regNumber)
              val clientRef = csvEscape(c.clientReference)
              s"$name,$regNumber,$clientRef"
            }

            val csvContent = (header +: rows).mkString("\n")

            Ok(csvContent)
              .as("text/csv")
              .withHeaders(
                "Content-Disposition" -> s"attachment; filename=${msgs("clientListSearch.download.filename")}"
              )
        }
        .recover { case e =>
          logger.error(s"downloadClientList failed: ${e.getMessage}", e)
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }

  private def csvEscape(raw: String): String = {
    val trimmed     = raw.dropWhile(_.isWhitespace)
    val dangerous   = trimmed.nonEmpty && "=+-@\t\r\n".contains(trimmed.head)
    val neutralised = if (dangerous) "'" + raw else raw
    "\"" + neutralised.replace("\"", "\"\"") + "\""
  }
}
