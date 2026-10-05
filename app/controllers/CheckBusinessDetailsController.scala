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

package controllers

import controllers.actions.*
import models.BusinessDetails
import pages.BusinessDetailsPage
import play.api.Logging
import repositories.SessionRepository
import services.GamblingService
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CheckBusinessDetailsView

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
    with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = (agentOnly andThen getData andThen requireData).async { implicit request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    request.userAnswers.get(BusinessDetailsPage) match {
      case Some(details) => Future.successful(Ok(view(details)))
      case None          =>
        gamblingService.getAgentDetails.flatMap {
          case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) =>
            logger.info(s"agent details were not found for ${request.mgdRegNum}")
            Future.successful(Redirect(routes.PageNotFoundController.onPageLoad()))
          case Left(error)                                     => Future.failed(error)
          case Right(agentDetails)                             =>
            val details = BusinessDetails.fromAgentDetails(agentDetails)
            for {
              updated <- Future.fromTry(request.userAnswers.set(BusinessDetailsPage, details))
              _       <- sessionRepository.set(updated)
            } yield Ok(view(details))
        }
    }
  }
}
