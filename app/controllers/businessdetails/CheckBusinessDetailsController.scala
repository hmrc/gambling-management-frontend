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
import models.agent.ContactNumbers
import models.requests.DataRequest
import models.{BusinessDetails, GamblingError}
import pages.BusinessDetailsPage
import pages.businessdetails.{BusinessNamePage, ChangeContactNumbersPage, ChangeEmailAddressPage, ChangeFaxNumberPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.businessdetails.CheckBusinessDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckBusinessDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  agentOnly: AgentOnlyAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  gamblingService: GamblingService,
  view: CheckBusinessDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (agentOnly andThen getData andThen requireData).async { implicit request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    request.userAnswers.get(BusinessDetailsPage) match {
      case Some(details) => Future.successful(Ok(view(details)))
      case None          =>
        gamblingService.getAgentDetails.flatMap {
          case Left(GamblingError.NotFound) =>
            logger.info(s"Agent details were not found for ${request.mgdRegNum}")
            Future.successful(Redirect(controllers.routes.PageNotFoundController.onPageLoad()))
          case Left(_)                      =>
            Future.successful(Redirect(controllers.routes.SystemErrorController.onPageLoad()))
          case Right(agentDetails)          =>
            val bd = BusinessDetails.fromAgentDetails(agentDetails)
            updateUserAnswersFromBusinessDetails(bd, request).map(answers => Ok(view(bd)))
        }
    }
  }

  private def updateUserAnswersFromBusinessDetails(bd: BusinessDetails, request: DataRequest[_]) =
    for {
      updatedBD <- Future.fromTry(request.userAnswers.set(BusinessDetailsPage, bd))

      updatedName <-
        (updatedBD.get(BusinessNamePage), bd.businessName) match {
          case (None, Some(businessName)) =>
            Future.fromTry(updatedBD.set(BusinessNamePage, businessName))
          case _                          => Future.successful(updatedBD)
        }

      updatedContactNums <-
        (updatedName.get(ChangeContactNumbersPage), bd.phoneNumber, bd.mobileNumber) match {
          case (None, phone, mobile) if phone.isDefined | mobile.isDefined =>
            Future.fromTry(updatedName.set(ChangeContactNumbersPage, ContactNumbers(phone, mobile)))
          case _                                                           => Future.successful(updatedName)
        }

      updatedFax <-
        (updatedContactNums.get(ChangeFaxNumberPage), bd.faxNumber) match {
          case (None, Some(faxNumber)) =>
            Future.fromTry(updatedContactNums.set(ChangeFaxNumberPage, faxNumber))
          case _                       => Future.successful(updatedContactNums)
        }

      updatedEmail <-
        (updatedFax.get(ChangeEmailAddressPage), bd.emailAddress) match {
          case (None, Some(emailAddress)) =>
            Future.fromTry(updatedFax.set(ChangeEmailAddressPage, emailAddress))
          case _                          => Future.successful(updatedFax)
        }

      _ <- sessionRepository.set(updatedEmail)
    } yield updatedEmail

}
