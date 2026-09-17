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

package controllers.agent

import controllers.actions.{AuthorisedAction, ClientListStatusGuard, DataRequiredAction, DataRetrievalAction, HasClientGuard}
import models.agent.AgentClient
import models.audit.ClientDetailsRetrievedAuditEventModel
import models.requests.DataRequest
import navigation.ClientListCheckNavigator
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.agent.AgentLandingView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AgentLandingController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  clientListStatusGuard: ClientListStatusGuard,
  clientListCheckNavigator: ClientListCheckNavigator,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  hasClientGuard: HasClientGuard,
  auditService: AuditService,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  view: AgentLandingView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(uniqueId: String): Action[AnyContent] =
    (authorise
      andThen clientListStatusGuard.groupB(clientListCheckNavigator.agentLanding(uniqueId))
      andThen getData
      andThen requireData
      andThen hasClientGuard.forInstanceId(uniqueId)).async { implicit request =>

      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      AgentClientsPage.findClient(request.userAnswers, uniqueId) match {
        case Some(client) =>
          loadLandingPage(uniqueId, client)

        case None =>
          logger.warn(s"[AgentLandingController][onPageLoad] Missing client in userAnswers for uniqueId=$uniqueId")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def loadLandingPage(
    uniqueId: String,
    client: AgentClient
  )(using request: DataRequest[?], hc: HeaderCarrier): Future[Result] =
    (for {
      _                  <- auditClientDetailsRetrieved(client, uniqueId)
      updatedUserAnswers <- Future.fromTry(request.userAnswers.set(SelectedClientPage, uniqueId))
      _                  <- sessionRepository.set(updatedUserAnswers)
    } yield Ok(
      view(
        clientName = client.clientName.getOrElse(""),
        regNumber = client.regNumber
      )
    )).recover { case NonFatal(ex) =>
      logger.error(s"[AgentLandingController][onPageLoad] Failed for uniqueId=$uniqueId", ex)
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  private def auditClientDetailsRetrieved(
    client: AgentClient,
    uniqueId: String
  )(using request: DataRequest[?], hc: HeaderCarrier): Future[Unit] = {
    // Agent reference is not threaded into DataRequest in this lightweight flow; audit best-effort.
    given play.api.mvc.Request[?] = request

    val auditEvent = ClientDetailsRetrievedAuditEventModel(
      agentReference = "",
      regime = client.regime,
      regNumber = client.regNumber
    )
    auditService
      .sendEvent(auditEvent)
      .map(_ => ())
      .recover { case NonFatal(ex) =>
        logger.error(s"[AgentLandingController] failed to send ClientDetailsRetrieved audit for uniqueId=$uniqueId", ex)
        ()
      }
  }
}
