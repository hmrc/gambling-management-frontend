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

import models.agent.ClientListCheckPolicy
import models.requests.AuthorisedRequest
import play.api.mvc.Result

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientListCheckEnforcer @Inject() (
  policyResolver: ClientListCheckPolicyResolver,
  clientListStatusGuard: ClientListStatusGuard,
  hasClientGuard: HasClientGuard
)(using ec: ExecutionContext) {

  def apply[A](request: AuthorisedRequest[A])(block: AuthorisedRequest[A] => Future[Result]): Future[Result] =
    if !request.isAgent then block(request)
    else
      policyResolver.resolve(request) match {
        case ClientListCheckPolicy.GroupA                                =>
          clientListStatusGuard.checkGroupA(request).flatMap {
            case Some(result) =>
              Future.successful(result)

            case None if policyResolver.shouldRunCentralHasClient(request) =>
              hasClientGuard.check(request).flatMap {
                case Some(result) =>
                  Future.successful(result)
                case None         =>
                  block(request)
              }

            case None =>
              block(request)
          }
        case ClientListCheckPolicy.GroupB | ClientListCheckPolicy.Exempt =>
          block(request)
      }
}
