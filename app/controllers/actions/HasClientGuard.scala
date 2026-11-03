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
          logger.warn("UserAnswers missing")
          Future.successful(Some(systemError))

        case Some(userAnswers) =>
          userAnswers.get(SelectedClientPage) match {
            case None =>
              logger.warn("selected client missing in UserAnswers")
              Future.successful(Some(systemError))

            case Some(regNumber) =>
              AgentClientsPage.findInClientList(userAnswers, regNumber) match {
                case None =>
                  logger.warn(s"Client not found in Client List for regNumber: $regNumber")
                  Future.successful(Some(systemError))

                case Some(client) =>
                  checkClient(client.regime, client.regNumber, regNumber)
              }
          }
      }
      .recover { case NonFatal(ex) =>
        logger.error("hasClient check failed", ex)
        Some(systemError)
      }
  }

  def forRegNumber(regNumber: String): ActionFilter[DataRequest] =
    new ActionFilter[DataRequest] {

      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] =
        checkForRegNumber(request, regNumber)
    }

  def currentClient: ActionFilter[DataRequest] =
    new ActionFilter[DataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] =
        checkCurrentClient(request)
    }

  def currentClientAuthorised: ActionFilter[AuthorisedRequest] =
    new ActionFilter[AuthorisedRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: AuthorisedRequest[A]): Future[Option[Result]] =
        if !request.isAgent then Future.successful(None) else check(request)
    }

  private[actions] def checkForRegNumber[A](request: DataRequest[A], regNumber: String): Future[Option[Result]] =
    if !request.isAgent then Future.successful(None)
    else {
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      given Request[?]    = request

      AgentClientsPage.findInClientList(request.userAnswers, regNumber) match {

        case Some(client) =>
          checkClient(client.regime, client.regNumber, regNumber)

        case None =>
          logger.warn(s"Client not found in Client List for regNumber: $regNumber")
          Future.successful(Some(systemError))
      }
    }

  private[actions] def checkCurrentClient[A](request: DataRequest[A]): Future[Option[Result]] =
    if !request.isAgent then Future.successful(None)
    else
      request.userAnswers.get(SelectedClientPage) match {
        case Some(regNumber) =>
          checkForRegNumber(request, regNumber)
        case None            =>
          logger.warn("selected client missing in UserAnswers")
          Future.successful(Some(systemError))
      }

  private def checkClient(
    regimeFromClientList: String,
    regNumberFromClientList: String,
    regNumber: String
  )(using HeaderCarrier, Request[?]): Future[Option[Result]] =
    if regimeFromClientList.isEmpty || regNumberFromClientList.isEmpty then {
      logger.warn(s"regime or regNumber is empty in Client List for regNumber: $regNumber")
      Future.successful(Some(systemError))
    } else
      gamblingService
        .hasClient(regimeFromClientList, regNumberFromClientList)
        .flatMap {
          case true =>
            Future.successful(None)

          case false =>
            logger.warn(s"Agent no longer authorised for regNumber: $regNumber")
            auditService
              .sendEvent(AuthFailureAuditEventModel())
              .map(_ => Some(agentLostAccess))
              .recover { case NonFatal(ex) =>
                logger.error("failed to send authoriseServiceGuardFailure audit", ex)
                Some(agentLostAccess)
              }
        }
        .recover { case NonFatal(ex) =>
          logger.error(s"hasClient check failed for regNumber: $regNumber", ex)
          Some(systemError)
        }
}
