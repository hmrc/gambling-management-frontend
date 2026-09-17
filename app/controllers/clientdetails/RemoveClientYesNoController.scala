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
import forms.clientdetails.RemoveClientYesNoFormProvider
import navigation.ClientListCheckNavigator
import pages.clientdetails.RemoveClientYesNoPage
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.ManageService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.clientdetails.RemoveClientYesNoView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveClientYesNoController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  authorise: AuthorisedAction,
  clientListStatusGuard: ClientListStatusGuard,
  clientListCheckNavigator: ClientListCheckNavigator,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  hasClientGuard: HasClientGuard,
  formProvider: RemoveClientYesNoFormProvider,
  manageService: ManageService,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveClientYesNoView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(uniqueId: String): Action[AnyContent] =
    (authorise
      andThen clientListStatusGuard.groupB(clientListCheckNavigator.removeClient(uniqueId))
      andThen getData
      andThen requireData
      andThen hasClientGuard.forInstanceId(uniqueId)).async { implicit request =>
      AgentClientsPage.findClient(request.userAnswers, uniqueId) match {
        case Some(client) =>
          val preparedForm = request.userAnswers.get(RemoveClientYesNoPage).fold(form)(form.fill)
          Future.successful(Ok(view(client.clientName.getOrElse(""), preparedForm, uniqueId)))

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(uniqueId: String): Action[AnyContent] =
    (authorise
      andThen getData
      andThen requireData
      andThen hasClientGuard.forInstanceId(uniqueId)).async { implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      AgentClientsPage.findClient(request.userAnswers, uniqueId) match {
        case Some(client) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(view(client.clientName.getOrElse(""), formWithErrors, uniqueId))),
              value =>
                if (value) removeAndConfirm(uniqueId)
                else Future.successful(Redirect(routes.ManageClientDetailsController.onPageLoad()))
            )

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def removeAndConfirm(uniqueId: String)(using
    request: models.requests.DataRequest[?],
    hc: HeaderCarrier
  ): Future[play.api.mvc.Result] =
    (for {
      _                <- manageService.removeClient(uniqueId, request.userAnswers)
      clearedClients   <- Future.fromTry(request.userAnswers.remove(AgentClientsPage))
      clearedSelection <- Future.fromTry(clearedClients.remove(SelectedClientPage))
      _                <- sessionRepository.set(clearedSelection)
    } yield Redirect(routes.ClientRemovedController.onPageLoad()))
      .recover { case ex =>
        logger.error(s"[RemoveClientYesNoController][onSubmit] Failed to remove client uniqueId=$uniqueId", ex)
        Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
}
