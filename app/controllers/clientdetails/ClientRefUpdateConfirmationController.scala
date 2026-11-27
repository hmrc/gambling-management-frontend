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

package controllers.clientdetails

import controllers.actions.*
import pages.clientdetails.ClientReferencePage
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ManageService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.agent.client.ClientDetailsSummary
import views.html.clientdetails.ClientRefUpdateConfirmationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientRefUpdateConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  clientListCheck: ClientListCheckAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  hasClientGuard: HasClientGuard,
  manageService: ManageService,
  view: ClientRefUpdateConfirmationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (authorise
      andThen clientListCheck
      andThen getData
      andThen requireData
      andThen hasClientGuard.currentClient).async { implicit request =>
      request.userAnswers
        .get(SelectedClientPage)
        .flatMap(regNum => AgentClientsPage.findInClientList(request.userAnswers, regNum)) match {
        case Some(client) =>
          request.userAnswers.get(ClientReferencePage) match {
            case Some(clientReference) =>
              Future.successful(
                Ok(
                  view(
                    regNumber = client.regNumber,
                    summaryList = ClientDetailsSummary.rowForConfirmation(client.regNumber, clientReference)
                  )
                )
              )
            case None                  => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        case None         =>
          logger.warn("no selected client in userAnswers")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit: Action[AnyContent] =
    (authorise
      andThen clientListCheck
      andThen getData
      andThen requireData
      andThen hasClientGuard.currentClient).async { implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      request.userAnswers
        .get(SelectedClientPage)
        .flatMap(regNum => AgentClientsPage.findInClientList(request.userAnswers, regNum)) match {
        case Some(client) =>
          request.userAnswers.get(ClientReferencePage) match {
            case Some(clientReference) =>
              (for {
                _ <- manageService.updateClient(client.regNumber, request.userAnswers, clientReference.trim)
              } yield Redirect(routes.ManageClientDetailsController.onPageLoad()))
                .recover { case ex =>
                  logger.error(s"Failed to update client reference for regNumber ${client.regNumber}", ex)
                  Redirect(controllers.routes.SystemErrorController.onPageLoad())
                }
            case None                  =>
              logger.warn("no selected Client Reference in userAnswers")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        case None         =>
          logger.warn("no selected client in userAnswers")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
