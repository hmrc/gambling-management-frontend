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

import controllers.actions.{AuthorisedAction, ClientListCheckAction, DataRequiredAction, DataRetrievalAction, HasClientGuard}
import forms.clientdetails.RemoveClientFormProvider
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
import views.html.clientdetails.RemoveClientView
import pages.clientdetails.RemoveClientPage

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveClientController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  authorise: AuthorisedAction,
  clientListCheck: ClientListCheckAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  hasClientGuard: HasClientGuard,
  formProvider: RemoveClientFormProvider,
  manageService: ManageService,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveClientView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(regNumber: String): Action[AnyContent] =
    (authorise
      andThen clientListCheck
      andThen getData
      andThen requireData
      andThen hasClientGuard.forRegNumber(regNumber)).async { implicit request =>
      AgentClientsPage.findInClientList(request.userAnswers, regNumber) match {
        case Some(client) =>
          val preparedForm =
            request.userAnswers
              .get(RemoveClientPage)
              .fold(form)(form.fill)

          Future.successful(
            Ok(
              view(
                client.clientName.getOrElse(""),
                preparedForm,
                regNumber
              )
            )
          )

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(regNumber: String): Action[AnyContent] =
    (authorise
      andThen getData
      andThen requireData
      andThen hasClientGuard.forRegNumber(regNumber)).async { implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      AgentClientsPage.findInClientList(request.userAnswers, regNumber) match {
        case Some(client) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(view(client.clientName.getOrElse(""), formWithErrors, regNumber))),
              value =>
                Future
                  .fromTry(
                    request.userAnswers.set(RemoveClientPage, value)
                  )
                  .flatMap { updatedAnswers =>
                    sessionRepository.set(updatedAnswers).flatMap { _ =>
                      if (value) {
                        removeAndConfirm(regNumber, updatedAnswers)
                      } else {
                        for {
                          clearedAnswers <- Future.fromTry(
                                              updatedAnswers.remove(RemoveClientPage)
                                            )
                          _              <- sessionRepository.set(clearedAnswers)
                        } yield Redirect(
                          controllers.agent.routes.ClientListController.onPageLoad()
                        )
                      }
                    }
                  }
            )
        case None         =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def removeAndConfirm(regNumber: String, userAnswers: models.UserAnswers)(using
    hc: HeaderCarrier
  ): Future[play.api.mvc.Result] =
    (for {
      _                <- manageService.removeClient(regNumber, userAnswers)
      clients           = userAnswers.get(AgentClientsPage).getOrElse(List.empty)
      remainingClients  = clients.filterNot(_.regNumber == regNumber)
      updatedClients   <- Future.fromTry(userAnswers.set(AgentClientsPage, remainingClients))
      clearedSelection <- Future.fromTry(updatedClients.remove(SelectedClientPage))
      clearedAnswer    <- Future.fromTry(clearedSelection.remove(RemoveClientPage))
      _                <- sessionRepository.set(clearedAnswer)
    } yield Redirect(controllers.agent.routes.ClientListController.onPageLoad()))
      .recover { case ex =>
        logger.error(s"Failed to remove client regNumber=$regNumber", ex)
        Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
}
