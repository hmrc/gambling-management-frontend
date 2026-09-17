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

import models.requests.{AuthorisedRequest, DataRequest}
import play.api.mvc.{ActionFilter, Call, Result}
import services.{AuditService, GamblingService}
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

/** A ClientListStatusGuard whose guards always pass (return None). */
class PassThroughStatusGuard(service: GamblingService)(implicit ec: ExecutionContext)
    extends ClientListStatusGuard(service) {

  private def pass: ActionFilter[AuthorisedRequest] = new ActionFilter[AuthorisedRequest] {
    override protected def executionContext: ExecutionContext                              = ec
    override protected def filter[A](request: AuthorisedRequest[A]): Future[Option[Result]] = Future.successful(None)
  }

  override def groupB(securityCheckCall: Call): ActionFilter[AuthorisedRequest] = pass
  override def checkGroupA[A](request: AuthorisedRequest[A]): Future[Option[Result]] = Future.successful(None)
}

/** A HasClientGuard whose guards always pass (return None). */
class PassThroughHasClientGuard(
  service: GamblingService,
  sessionRepository: SessionRepository,
  audit: AuditService
)(implicit ec: ExecutionContext)
    extends HasClientGuard(service, sessionRepository, audit) {

  private def pass: ActionFilter[DataRequest] = new ActionFilter[DataRequest] {
    override protected def executionContext: ExecutionContext                          = ec
    override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = Future.successful(None)
  }

  override def forInstanceId(instanceId: String): ActionFilter[DataRequest] = pass
  override def currentClient: ActionFilter[DataRequest]                     = pass
}
