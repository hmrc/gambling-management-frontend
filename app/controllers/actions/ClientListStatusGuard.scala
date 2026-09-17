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

import controllers.actions.ClientListCheckRedirects.{agentLostAccess, systemError}
import models.agent.ClientListStatus
import models.requests.AuthorisedRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Call, Result}
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ClientListStatusGuard @Inject() (
  gamblingService: GamblingService
)(using ec: ExecutionContext)
    extends Logging {

  def checkGroupA[A](request: AuthorisedRequest[A]): Future[Option[Result]] =
    check(request) {
      case ClientListStatus.Succeeded                                                                =>
        None
      case ClientListStatus.InProgress | ClientListStatus.Failed | ClientListStatus.InitiateDownload =>
        Some(agentLostAccess)
    }

  def groupB(securityCheckCall: Call): ActionFilter[AuthorisedRequest] =
    new ActionFilter[AuthorisedRequest] {

      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: AuthorisedRequest[A]): Future[Option[Result]] =
        check(request) {
          case ClientListStatus.Succeeded                                  =>
            None
          case ClientListStatus.InProgress                                 =>
            Some(Redirect(securityCheckCall))
          case ClientListStatus.Failed | ClientListStatus.InitiateDownload =>
            Some(systemError)
        }
    }

  private def check[A](
    request: AuthorisedRequest[A]
  )(handleStatus: ClientListStatus => Option[Result]): Future[Option[Result]] = {
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    gamblingService.startClientListRetrieval
      .map(handleStatus)
      .recover { case NonFatal(e) =>
        logger.error("[ClientListStatusGuard] client list check failed", e)
        Some(systemError)
      }
  }
}
