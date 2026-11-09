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
import models.{BusinessDetails, GamblingError, UserAnswers}
import pages.businessdetails.{BusinessNamePage, ChangeContactNumbersPage, ChangeEmailAddressPage, ChangeFaxNumberPage}
import pages.{BusinessDetailsPage, QuestionPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.businessdetails.CheckBusinessDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

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

  private def updateUserAnswersFromBusinessDetails(
    bd: BusinessDetails,
    request: DataRequest[_]
  ): Future[UserAnswers] = {
    val prefilled =
      for {
        withDetails <- request.userAnswers.set(BusinessDetailsPage, bd)
        withName    <- setIfAbsent(withDetails, BusinessNamePage, bd.businessName)
        withContact <- setIfAbsent(withName, ChangeContactNumbersPage, contactNumbers(bd))
        withFax     <- setIfAbsent(withContact, ChangeFaxNumberPage, bd.faxNumber)
        withEmail   <- setIfAbsent(withFax, ChangeEmailAddressPage, bd.emailAddress)
      } yield withEmail

    for {
      answers <- Future.fromTry(prefilled)
      _       <- sessionRepository.set(answers)
    } yield answers
  }

  private def setIfAbsent[A](answers: UserAnswers, page: QuestionPage[A], value: Option[A])(implicit
    reads: Reads[A],
    writes: Writes[A]
  ): Try[UserAnswers] =
    value match {
      case Some(v) if answers.get(page).isEmpty => answers.set(page, v)
      case _                                    => Success(answers)
    }

  private def contactNumbers(bd: BusinessDetails): Option[ContactNumbers] =
    Option.when(bd.phoneNumber.isDefined || bd.mobileNumber.isDefined)(
      ContactNumbers(bd.phoneNumber, bd.mobileNumber)
    )
}
