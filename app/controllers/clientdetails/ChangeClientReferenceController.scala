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
import forms.clientdetails.ChangeClientReferenceFormProvider
import navigation.ClientListCheckNavigator
import pages.clientdetails.ChangeClientReferencePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.ManageService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.clientdetails.ChangeClientReferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeClientReferenceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  authorise: AuthorisedAction,
  clientListStatusGuard: ClientListStatusGuard,
  clientListCheckNavigator: ClientListCheckNavigator,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  hasClientGuard: HasClientGuard,
  formProvider: ChangeClientReferenceFormProvider,
  manageService: ManageService,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeClientReferenceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = formProvider()

  def onPageLoad(regNumber: String): Action[AnyContent] =
    (authorise
      andThen clientListStatusGuard.groupB(clientListCheckNavigator.changeClientReference(regNumber))
      andThen getData
      andThen requireData
      andThen hasClientGuard.forInstanceId(regNumber)).async { implicit request =>
      val preparedForm = request.userAnswers.get(ChangeClientReferencePage).fold(form)(form.fill)
      Future.successful(Ok(view(preparedForm, regNumber)))
    }

  def onSubmit(regNumber: String): Action[AnyContent] =
    (authorise
      andThen clientListStatusGuard.groupB(clientListCheckNavigator.changeClientReference(regNumber))
      andThen getData
      andThen requireData
      andThen hasClientGuard.forInstanceId(regNumber)).async { implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, regNumber))),
          value =>
            (for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeClientReferencePage, value.trim))
              _              <- manageService.updateClient(regNumber, updatedAnswers, value.trim)
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(routes.ClientRefUpdateConfirmationController.onPageLoad()))
              .recover { case ex =>
                logger.error(s"Failed to update client reference for regNumber $regNumber", ex)
                Redirect(controllers.routes.SystemErrorController.onPageLoad())
              }
        )
    }
}
