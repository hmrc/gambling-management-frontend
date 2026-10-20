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

import controllers.actions.*
import controllers.businessdetails.ChangeContactNumbersController.highlightFirstIfBothBlank
import forms.businessdetails.ChangeContactNumbersFormProvider
import models.agent.ContactNumbers
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.BusinessDetailsPage
import pages.businessdetails.ChangeContactNumbersPage
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.businessdetails.ChangeContactNumbersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeContactNumbersController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  agentOnly: AgentOnlyAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ChangeContactNumbersFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeContactNumbersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[ContactNumbers] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (agentOnly andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(ChangeContactNumbersPage) match {
      case Some(value) => form.fill(value)
      case None        =>
        request.userAnswers.get(BusinessDetailsPage) match {
          case Some(details) => form.fill(ContactNumbers(details.phoneNumber, details.mobileNumber))
          case None          => form
        }
    }
    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (agentOnly andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(highlightFirstIfBothBlank(formWithErrors), mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeContactNumbersPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(ChangeContactNumbersPage, mode, updatedAnswers))
        )
    }
}

object ChangeContactNumbersController {

  def highlightFirstIfBothBlank(form: Form[ContactNumbers]): Form[ContactNumbers] =
    if (form.hasGlobalErrors) {
      form.copy(errors = Seq(FormError("phoneNumber", "changeContactNumbers.error.phoneRequired")))
    } else {
      form
    }
}
