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
import services.{ManageService, PaginationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.agent.ClientListViewModel
import views.html.agent.ClientListView

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientListController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  clientListCheck: ClientListCheckAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  manageService: ManageService,
  paginationService: PaginationService,
  val controllerComponents: MessagesControllerComponents,
  view: ClientListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen clientListCheck andThen getData).async { implicit request =>
      val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
      val searchQuery = request.getQueryString("search").getOrElse("")
      val pageParam   = request.getQueryString("page")
      val currentPage = pageParam.flatMap(_.toIntOption).getOrElse(1)

      manageService
        .resolveAndStoreAgentClients(userAnswers)
        .map {
          case (Nil, _)     =>
            Redirect(controllers.agent.routes.NoAuthorisedClientsController.onPageLoad())
          case (clients, _) =>
            val allClients      = ClientListViewModel.fromAgentClients(clients)
            val filteredClients = ClientListViewModel.filterByField(searchQuery, allClients)
            val baseUrl         = routes.ClientListController.onPageLoad().url

            // Preserve the search filter when navigating between pages
            val paginationBaseUrl =
              if (searchQuery.trim.nonEmpty) {
                val encodedSearch =
                  URLEncoder.encode(searchQuery.trim, StandardCharsets.UTF_8.toString)
                s"$baseUrl?search=$encodedSearch"
              } else {
                baseUrl
              }

            val paginationResult =
              paginationService.paginateClientList(
                allClients = filteredClients,
                currentPage = currentPage,
                baseUrl = paginationBaseUrl,
                sortBy = None,
                sortOrder = None
              )

            Ok(view(paginationResult.paginatedData, searchQuery, paginationResult.paginationViewModel))
        }
        .recover { case e =>
          logger.error("failed to resolve and store agent clients", e)
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }

  def downloadClientList(): Action[AnyContent] =
    (authorise andThen clientListCheck andThen getData andThen requireData).async { implicit request =>
      manageService
        .resolveAndStoreAgentClients(request.userAnswers)
        .map {
          case (Nil, _)        =>
            Redirect(controllers.agent.routes.NoAuthorisedClientsController.onPageLoad())
          case (clientList, _) =>
            val clientsToDownload = ClientListViewModel.fromAgentClients(clientList)

            implicit val msgs: Messages = messagesApi.preferred(request)

            val rows: Seq[String] = clientsToDownload.map { c =>
              val name      = csvEscape(c.clientName)
              val regNumber = csvEscape(c.regNumber)
              val clientRef = csvEscape(c.clientReference)
              s"$name,$regNumber,$clientRef"
            }

            // Prepend a UTF-8 BOM so spreadsheet tools (notably Excel on Windows) detect the
            // encoding and render accented/Welsh characters correctly.
            val bom        = "\uFEFF"
            val csvContent = bom + (csvHeader(msgs) +: rows).mkString("\n")

            Ok(csvContent)
              .as("text/csv; charset=UTF-8")
              .withHeaders(
                "Content-Disposition" -> s"attachment; filename=${msgs("clientList.download.filename")}"
              )
        }
        .recover { case e =>
          logger.error(s"downloadClientList failed: ${e.getMessage}", e)
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }

  private def csvHeader(implicit msgs: Messages): String = Seq(
    msgs("clientList.download.th.clientName"),
    msgs("clientList.download.th.regNumber"),
    msgs("clientList.download.th.clientReference")
  ).mkString(",")

  private def csvEscape(raw: String): String = {
    // Guard against CSV formula injection: a cell whose first non-whitespace character is one of
    // these is treated as a formula by spreadsheet tools, so neutralise it with a leading quote.
    val trimmed     = raw.dropWhile(_.isWhitespace)
    val dangerous   = trimmed.nonEmpty && "=+-@".contains(trimmed.head)
    val neutralised = if (dangerous) "'" + raw else raw

    // Wrap every field in double quotes (escaping any internal quotes by doubling)
    // so embedded commas, quotes and newlines don't break the CSV structure.
    "\"" + neutralised.replace("\"", "\"\"") + "\""
  }
}
