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
import models.agent.ContactNumbers
import models.businessdetails.BusinessContactDetailSubmission
import models.{CheckMode, Mode, NormalMode}
import pages.businessdetails.{BusinessContactDetailResponsePage, ChangeContactNumbersPage, ChangeEmailAddressPage, ChangeFaxNumberPage}
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
  agentOnly: AgentOnlyAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  gamblingConnector: GamblingConnector,
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
    (agentOnly andThen getData andThen requireData).async { implicit request =>

      val (phoneNumber, mobileNumber) = request.userAnswers.get(ChangeContactNumbersPage) match {
        case Some(ContactNumbers(phoneNumber, mobileNumber)) => (phoneNumber, mobileNumber)
        case _                                               => (Some(""), Some(""))
      }

      val submission =
        BusinessContactDetailSubmission(
          submitterEmail = None,
          phoneNumber = phoneNumber,
          mobileNumber = mobileNumber,
          faxNumber = request.userAnswers.get(ChangeFaxNumberPage),
          email = request.userAnswers.get(ChangeEmailAddressPage)
        )

      for {
        submissionResponse <- gamblingConnector.updateBusinessDetails(submission)
        clearedAnswers     <- Future.fromTry(
                                BusinessContactHelpers.clearChanges(request.userAnswers)
                              )

        updatedAnswers <- Future.fromTry(
                            clearedAnswers.set(
                              BusinessContactDetailResponsePage,
                              submissionResponse
                            )
                          )

        _ <- sessionRepository.set(updatedAnswers)
      } yield Redirect(
        controllers.businessdetails.routes.ChangeBusinessContactConfirmationController.onPageLoad()
      )
    }
}
