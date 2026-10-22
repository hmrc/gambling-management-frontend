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

import controllers.actions.{AgentOnlyAction, DataRequiredAction, DataRetrievalAction}
import pages.businessdetails.BusinessNameConfirmationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.businessdetails.ChangeConfirmationView

import javax.inject.Inject

class ChangeBusinessNameConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  agentOnly: AgentOnlyAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeConfirmationView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (agentOnly andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(BusinessNameConfirmationPage) match {
      case Some(response) =>
        Ok(view(response.acknowledgementReference, routes.CheckBusinessDetailsController.onPageLoad().url))
      case None           => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
