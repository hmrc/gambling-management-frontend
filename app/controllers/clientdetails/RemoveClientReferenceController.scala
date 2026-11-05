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
import forms.clientdetails.RemoveClientReferenceFormProvider
import pages.clientdetails.RemoveClientReferencePage
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
import views.html.clientdetails.RemoveClientReferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveClientReferenceController @Inject() (
                                                  override val messagesApi: MessagesApi,
                                                  sessionRepository: SessionRepository,
                                                  authorise: AuthorisedAction,
                                                  clientListCheck: ClientListCheckAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  hasClientGuard: HasClientGuard,
                                                  formProvider: RemoveClientReferenceFormProvider,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: RemoveClientReferenceView
                                                )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(regNumber: String): Action[AnyContent] =
    (authorise
      andThen clientListCheck
      andThen getData
      andThen requireData
      andThen hasClientGuard.forRegNumber(regNumber)).async { implicit request =>

      AgentClientsPage.findInClientList(request.userAnswers, regNumber) match {

        case Some(_) =>
          val preparedForm =
            request.userAnswers
              .get(RemoveClientReferencePage)
              .fold(form)(form.fill)

          Future.successful(
            Ok(
              view(
                preparedForm,
                regNumber
              )
            )
          )

        case None =>
          Future.successful(
            Redirect(
              controllers.routes.JourneyRecoveryController.onPageLoad()
            )
          )
      }
    }

  def onSubmit(regNumber: String): Action[AnyContent] =
    (authorise
      andThen clientListCheck
      andThen getData
      andThen requireData
      andThen hasClientGuard.forRegNumber(regNumber)).async { implicit request =>

      AgentClientsPage.findInClientList(request.userAnswers, regNumber) match {

        case Some(_) =>
  form
    .bindFromRequest()
    .fold(
      formWithErrors =>
        Future.successful(
          BadRequest(
            view(
              formWithErrors,
              regNumber
            )
          )
        ),
      value =>
        Future
          .fromTry(
            request.userAnswers.set(
              RemoveClientReferencePage,
              value
            )
          )
          .flatMap { updatedAnswers =>
            sessionRepository.set(updatedAnswers).flatMap { _ =>
              if (value) {
                // TODO: integration ticket
                clearAndRedirect(updatedAnswers)
              } else {
                clearAndRedirect(updatedAnswers)
              }
            }
          }
    )

        case None =>
          Future.successful(
            Redirect(
              controllers.routes.JourneyRecoveryController.onPageLoad()
            )
          )
      }
    }

  private def clearAndRedirect(
                                userAnswers: models.UserAnswers
                              ): Future[play.api.mvc.Result] =
    for {
      clearedAnswers <- Future.fromTry(
        userAnswers.remove(RemoveClientReferencePage)
      )

      _ <- sessionRepository.set(clearedAnswers)

    } yield Redirect(
      controllers.agent.routes.ClientListController.onPageLoad()
    )
}