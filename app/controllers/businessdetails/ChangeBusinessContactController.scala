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
import models.{CheckMode, Mode}
import pages.BusinessDetailsPage
import pages.businessdetails.{ChangeEmailAddressPage, ChangeFaxNumberPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.businessContact.{ContactNumbersSummary, EmailAddressSummary, FaxNumberSummary}
import viewmodels.govuk.all.stringToKey
import viewmodels.govuk.summarylist.*
import views.html.businessdetails.ChangeBusinessContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeBusinessContactController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 authorise: AuthorisedAction,
                                                 agentOnly: AgentOnlyAction,
                                                 getData: DataRetrievalAction,
                                                 requireData: DataRequiredAction,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: ChangeBusinessContactView
                                               )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (agentOnly andThen getData andThen requireData) { implicit request =>

    val answers = request.userAnswers
    val contactNumbers = SummaryListViewModel(rows = ContactNumbersSummary.rows(answers))
    val faxNumber = SummaryListViewModel(rows = FaxNumberSummary.rows(answers))
    val emailAddress = SummaryListViewModel(rows = EmailAddressSummary.rows(answers))

    Ok(view(contactNumbers, faxNumber, emailAddress, mode))
  }

  def onSubmit(): Action[AnyContent] =
    (authorise andThen getData andThen requireData).async { implicit request =>
      // TODO: Submit to CHRIS

      Future.successful(
        Redirect(
          controllers.routes.IndexController.onPageLoad()
        )
      )
    }
}
