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

  def onPageLoad(regNumber: String): Action[AnyContent] =
    (authorise
      andThen clientListStatusGuard.groupB(clientListCheckNavigator.agentLanding(regNumber))
      andThen getData
      andThen requireData
      andThen hasClientGuard.forInstanceId(regNumber)).async { implicit request =>

      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      AgentClientsPage.findClient(request.userAnswers, regNumber) match {
        case Some(client) =>
          loadLandingPage(regNumber, client)

        case None =>
          logger.warn(s"Missing client in userAnswers for regNumber=$regNumber")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def loadLandingPage(
    regNumber: String,
    client: AgentClient
  )(using request: DataRequest[?], hc: HeaderCarrier): Future[Result] =
    (for {
      _                  <- auditClientDetailsRetrieved(client, regNumber)
      updatedUserAnswers <- Future.fromTry(request.userAnswers.set(SelectedClientPage, regNumber))
      _                  <- sessionRepository.set(updatedUserAnswers)
    } yield Ok(
      view(
        clientName = client.clientName.getOrElse(""),
        regNumber = client.regNumber
      )
    )).recover { case NonFatal(ex) =>
      logger.error(s"Failed for regNumber=$regNumber", ex)
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  private def auditClientDetailsRetrieved(
    client: AgentClient,
    regNumber: String
  )(using request: DataRequest[?], hc: HeaderCarrier): Future[Unit] = {
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
        logger.error(
          s"failed to send ClientDetailsRetrieved audit for regNumber=$regNumber",
          ex
        )
        ()
      }
  }
}
