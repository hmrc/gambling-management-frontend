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

package services

import connectors.GamblingConnector
import models.UserAnswers
import models.agent.{AgentClient, UpdateAgentClientRequest}
import models.requests.RemoveAgentClientRequest
import pages.AgentClientsPage
import play.api.Logging
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageService @Inject() (
  connector: GamblingConnector,
  sessionRepository: SessionRepository
)(using ExecutionContext)
    extends Logging {

  def resolveAndStoreAgentClients(
    userAnswers: UserAnswers
  )(using HeaderCarrier): Future[(List[AgentClient], UserAnswers)] =
    userAnswers.get(AgentClientsPage) match {
      case Some(clientList) => Future.successful((clientList, userAnswers))
      case None             =>
        logger.info("cache-miss: fetching agent clients from backend")
        for {
          clients        <- connector.getAllClients
          updatedAnswers <- Future.fromTry(userAnswers.set(AgentClientsPage, clients))
          _              <- sessionRepository.set(updatedAnswers)
        } yield (clients, updatedAnswers)
    }

  def updateClient(regNumber: String, ua: UserAnswers, clientRef: String)(using HeaderCarrier): Future[Unit] =
    ua.get(AgentClientsPage).flatMap(_.find(_.regNumber == regNumber)) match {
      case Some(client) =>
        connector.updateClient(UpdateAgentClientRequest(client.regime, client.regNumber, clientRef))
      case None         =>
        logger.error(s"no client found with regNumber $regNumber in AgentClientsPage")
        Future.failed(new RuntimeException(s"No client found with regNumber $regNumber in AgentClientsPage"))
    }

  def removeClient(regNumber: String, ua: UserAnswers)(using HeaderCarrier): Future[Unit] =
    ua.get(AgentClientsPage).flatMap(_.find(_.regNumber == regNumber)) match {
      case Some(client) =>
        connector.removeClient(RemoveAgentClientRequest(client.regime, client.regNumber))
      case None         =>
        logger.error(s"missing client in AgentClientsPage")
        Future.failed(new RuntimeException("Missing client in AgentClientsPage"))
    }
}
