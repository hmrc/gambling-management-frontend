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
import controllers.actions.{AgentOnlyAction, DataRequiredAction, DataRetrievalAction}
import models.{Mode, UserAnswers}
import models.businessdetails.BusinessNameSubmission
import pages.BusinessDetailsPage
import pages.businessdetails.{BusinessNameConfirmationPage, BusinessNamePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.GamblingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.businessdetails.ConfirmBusinessNameView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ConfirmBusinessNameController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  agentOnly: AgentOnlyAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  gamblingService: GamblingService,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmBusinessNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def changeBusinessNameRoute = routes.BusinessNameController.onPageLoad().url

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (agentOnly andThen getData andThen requireData) { implicit request =>
      request.userAnswers.get(BusinessNamePage) match {
        case None       => Redirect(changeBusinessNameRoute)
        case Some(name) => Ok(view(name, changeBusinessNameRoute))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (agentOnly andThen getData andThen requireData).async { implicit request =>
      request.userAnswers
        .get(BusinessNamePage)
        .fold(Future.successful(Redirect(changeBusinessNameRoute)))(name =>
          val submission = BusinessNameSubmission(fullName = name, submitterEmail = None)

          for {
            submissionResponse <- gamblingService.updateBusinessName(submission)
            withConfirmation   <-
              Future.fromTry(request.userAnswers.set(BusinessNameConfirmationPage, submissionResponse))
            // Reflect the confirmed name onto the stored business details so the Check business details
            // screen shows it, rather than the stale backend read-model value.
            updatedAnswers     <- Future.fromTry(
                                    withConfirmation
                                      .get(BusinessDetailsPage)
                                      .fold(Success(withConfirmation): Try[UserAnswers])(details =>
                                        withConfirmation.set(BusinessDetailsPage, details.copy(businessName = Some(name)))
                                      )
                                  )
            _                  <- sessionRepository.set(updatedAnswers)
          } yield Redirect(routes.ChangeBusinessNameConfirmationController.onPageLoad())
        )
    }
}
