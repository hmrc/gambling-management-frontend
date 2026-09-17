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

import controllers.actions.{AuthorisedAction, ClientListStatusGuard, DataRequiredAction, DataRetrievalAction, HasClientGuard}
import navigation.ClientListCheckNavigator
import pages.clientdetails.ChangeClientReferencePage
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.clientdetails.ManageClientDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageClientDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  clientListStatusGuard: ClientListStatusGuard,
  clientListCheckNavigator: ClientListCheckNavigator,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  hasClientGuard: HasClientGuard,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  view: ManageClientDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (authorise
      andThen clientListStatusGuard.groupB(clientListCheckNavigator.manageClientDetails)
      andThen getData
      andThen requireData
      andThen hasClientGuard.currentClient).async { implicit request =>
      request.userAnswers.get(SelectedClientPage).flatMap(id => AgentClientsPage.findClient(request.userAnswers, id)) match {
        case Some(client) =>
          val clientReference = client.agentOwnRef.getOrElse("")
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeClientReferencePage, clientReference))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Ok(
            view(
              uniqueId = client.uniqueId,
              clientName = client.clientName.getOrElse(""),
              regNumber = client.regNumber,
              clientReference = clientReference
            )
          )

        case None =>
          logger.warn("[ManageClientDetailsController][onPageLoad] no selected client in userAnswers")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
