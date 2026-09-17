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

import controllers.actions.ClientListCheckRedirects.systemError
import models.audit.AuthFailureAuditEventModel
import models.requests.{AuthorisedRequest, DataRequest}
import pages.{AgentClientsPage, SelectedClientPage}
import play.api.Logging
import play.api.mvc.{ActionFilter, Request, Result}
import repositories.SessionRepository
import services.{AuditService, GamblingService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class HasClientGuard @Inject() (
  gamblingService: GamblingService,
  sessionRepository: SessionRepository,
  auditService: AuditService
)(using ec: ExecutionContext)
    extends Logging {

  private[actions] def check[A](request: AuthorisedRequest[A]): Future[Option[Result]] = {
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    given Request[?]    = request

    sessionRepository
      .get(request.userId)
      .flatMap {
        case None =>
          logger.warn("[HasClientGuard] UserAnswers missing")
          Future.successful(Some(systemError))

        case Some(userAnswers) =>
          userAnswers.get(SelectedClientPage) match {
            case None =>
              logger.warn("[HasClientGuard] selected client missing in UserAnswers")
              Future.successful(Some(systemError))

            case Some(instanceId) =>
              AgentClientsPage.findClient(userAnswers, instanceId) match {
                case None =>
                  logger.warn(s"[HasClientGuard] client not found for instanceId: $instanceId")
                  Future.successful(Some(systemError))

                case Some(client) =>
                  checkClient(client.regime, client.regNumber, instanceId)
              }
          }
      }
      .recover { case NonFatal(ex) =>
        logger.error("[HasClientGuard] hasClient check failed", ex)
        Some(systemError)
      }
  }

  def forInstanceId(instanceId: String): ActionFilter[DataRequest] =
    new ActionFilter[DataRequest] {

      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] =
        checkForInstanceId(request, instanceId)
    }

  def currentClient: ActionFilter[DataRequest] =
    new ActionFilter[DataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] =
        checkCurrentClient(request)
    }

  private[actions] def checkForInstanceId[A](request: DataRequest[A], instanceId: String): Future[Option[Result]] =
    if !request.isAgent then Future.successful(None)
    else {
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      given Request[?]    = request

      AgentClientsPage.findClient(request.userAnswers, instanceId) match {

        case Some(client) =>
          checkClient(client.regime, client.regNumber, instanceId)

        case None =>
          logger.warn(s"[HasClientGuard] client not found for instanceId: $instanceId")
          Future.successful(Some(systemError))
      }
    }

  private[actions] def checkCurrentClient[A](request: DataRequest[A]): Future[Option[Result]] =
    request.userAnswers.get(SelectedClientPage) match {
      case Some(instanceId) =>
        checkForInstanceId(request, instanceId)
      case None             =>
        logger.warn("[HasClientGuard] selected client missing in UserAnswers")
        Future.successful(Some(systemError))
    }

  private def checkClient(
    regime: String,
    regNumber: String,
    instanceId: String
  )(using HeaderCarrier, Request[?]): Future[Option[Result]] =
    if regime.isEmpty || regNumber.isEmpty then {
      logger.warn(s"[HasClientGuard] regime/regNumber is empty for instanceId: $instanceId")
      Future.successful(Some(systemError))
    } else
      gamblingService
        .hasClient(regime, regNumber)
        .flatMap {
          case true =>
            Future.successful(None)

          case false =>
            logger.warn(s"[HasClientGuard] Agent no longer authorised for instanceId: $instanceId")
            auditService
              .sendEvent(AuthFailureAuditEventModel())
              .map(_ => Some(systemError))
              .recover { case NonFatal(ex) =>
                logger.error("[HasClientGuard] failed to send authoriseServiceGuardFailure audit", ex)
                Some(systemError)
              }
        }
        .recover { case NonFatal(ex) =>
          logger.error(s"[HasClientGuard] hasClient check failed for instanceId: $instanceId", ex)
          Some(systemError)
        }
}
