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

package controllers.businessdetails

import com.google.inject.Inject
import controllers.actions.{AgentOnlyAction, AuthorisedAction, DataRequiredAction, DataRetrievalAction}
import forms.businessdetails.{ChangeFaxNumberFormProvider, RemoveFaxNumberFormProvider}
import models.Mode
import navigation.Navigator
import pages.businessdetails.{ChangeFaxNumberPage, RemoveFaxNumberPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.businessdetails.RemoveFaxNumberView

import scala.concurrent.{ExecutionContext, Future}

class RemoveFaxNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  agentOnly: AgentOnlyAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemoveFaxNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RemoveFaxNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (agentOnly andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(ChangeFaxNumberPage) match {
      case Some(faxNumber) =>
        val preparedForm = request.userAnswers.get(RemoveFaxNumberPage).fold(form)(form.fill)

        Ok(view(preparedForm, faxNumber, mode))

      case None =>
        Redirect(
          controllers.routes.JourneyRecoveryController.onPageLoad()
        )
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (agentOnly andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(ChangeFaxNumberPage) match {
        case Some(faxNumber) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, faxNumber, mode))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(
                                      request.userAnswers.set(RemoveFaxNumberPage, value)
                                    )
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(RemoveFaxNumberPage, mode, updatedAnswers))
            )

        case None =>
          Future.successful(
            Redirect(
              controllers.routes.JourneyRecoveryController.onPageLoad()
            )
          )
      }
    }
}
