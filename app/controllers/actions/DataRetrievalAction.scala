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

package controllers.actions

import javax.inject.Inject
import models.requests.{AuthorisedRequest, OptionalDataRequest}
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.mvc.ActionTransformer
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject() (
  val sessionRepository: SessionRepository
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction {

  override protected def transform[A](request: AuthorisedRequest[A]): Future[OptionalDataRequest[A]] = {
    // Agents key their session on the stable internalId (no client reg number until one is selected);
    // organisations continue to key on their mgdRegNum.
    val sessionKey = if request.isAgent then request.userId else request.mgdRegNum

    sessionRepository.get(sessionKey).map { userAnswers =>
      val effectiveRegNum =
        if request.isAgent then
          (for {
            ua       <- userAnswers
            selected <- ua.get(SelectedClientPage)
            client   <- AgentClientsPage.findClient(ua, selected)
          } yield client.regNumber).getOrElse(request.mgdRegNum)
        else request.mgdRegNum

      OptionalDataRequest(request.request, effectiveRegNum, userAnswers, request.userId, request.isAgent)
    }
  }
}

trait DataRetrievalAction extends ActionTransformer[AuthorisedRequest, OptionalDataRequest]
