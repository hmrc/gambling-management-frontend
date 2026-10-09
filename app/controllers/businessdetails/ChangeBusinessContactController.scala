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

import connectors.GamblingConnector
import controllers.actions.*
<<<<<<< HEAD
import forms.businessdetails.ChangeBusinessContactFormProvider
import models.Mode
import pages.businessdetails.ChangeBusinessContactPage
=======
import models.businessdetails.BusinessContactDetailSubmission
import models.{CheckMode, Mode}
import pages.businessdetails.{ChangeEmailAddressPage, ChangeFaxNumberPage}
>>>>>>> d6f4eae (Added response/submit models, controller and connector changes)
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.businessContact.{BusinessContactHelpers, ContactNumbersSummary, EmailAddressSummary, FaxNumberSummary}
import viewmodels.govuk.summarylist.*
import views.html.businessdetails.ChangeBusinessContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeBusinessContactController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  authorise: AuthorisedAction,
  agentOnly: AgentOnlyAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
<<<<<<< HEAD
  formProvider: ChangeBusinessContactFormProvider,
=======
  gamblingConnector: GamblingConnector,
>>>>>>> d6f4eae (Added response/submit models, controller and connector changes)
  val controllerComponents: MessagesControllerComponents,
  view: ChangeBusinessContactView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (agentOnly andThen getData andThen requireData) { implicit request =>

    val answers     = request.userAnswers
    val summaryList = SummaryListViewModel(
      rows = ContactNumbersSummary.rows(answers) ++
        FaxNumberSummary.rows(answers) ++
        EmailAddressSummary.rows(answers)
    )

    Ok(view(summaryList, BusinessContactHelpers.hasChanges(answers), mode))
  }

  def onSubmit(): Action[AnyContent] =
<<<<<<< HEAD
    (agentOnly andThen getData andThen requireData).async { implicit request =>
      // TODO: Submit to CHRIS

      for {
        updatedAnswers <- Future.fromTry(BusinessContactHelpers.clearChanges(request.userAnswers))
        _              <- sessionRepository.set(updatedAnswers)
      } yield Redirect(controllers.routes.IndexController.onPageLoad())
=======
    (authorise andThen getData andThen requireData).async { implicit request =>

      val submission =
        BusinessContactDetailSubmission(
          submitterEmail = None, // TODO if source found
          phoneNumber = Some("0191 202 2500"), // TODO replace with > request.userAnswers.get(ChangeContactNumberPage)
          mobileNumber = Some("07890 123 456"), // TODO replace with > request.userAnswers.get(ChangeContactNumberPage)
          faxNumber = request.userAnswers.get(ChangeFaxNumberPage),
          email = request.userAnswers.get(ChangeEmailAddressPage)
        )

      for {
        response <- gamblingConnector.updateBusinessDetails(submission)
      } yield {
        Redirect(
          controllers.routes.IndexController.onPageLoad()
        )
      }
>>>>>>> d6f4eae (Added response/submit models, controller and connector changes)
    }

}
