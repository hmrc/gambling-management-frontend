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

import com.google.inject.ImplementedBy
import controllers.actions.ClientListCheckRedirects.systemError
import models.agent.ClientListStatus
import models.requests.AuthorisedRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.GamblingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[DefaultClientListCheckAction])
trait ClientListCheckAction extends ActionFilter[AuthorisedRequest]

@Singleton
class DefaultClientListCheckAction @Inject() (
  gamblingService: GamblingService
)(using ec: ExecutionContext)
    extends ClientListCheckAction
    with Logging {

  override protected def executionContext: ExecutionContext = ec

  override protected def filter[A](request: AuthorisedRequest[A]): Future[Option[Result]] =
    if !request.isAgent then Future.successful(None)
    else {
      given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      gamblingService.startClientListRetrieval
        .map {
          case ClientListStatus.Succeeded                                      =>
            None
          case ClientListStatus.InProgress | ClientListStatus.InitiateDownload =>
            Some(Redirect(controllers.routes.SecurityCheckController.onClientListCheck(RedirectUrl(request.uri))))
          case ClientListStatus.Failed                                         =>
            Some(systemError)
        }
        .recover { case NonFatal(e) =>
          logger.error("client list check failed", e)
          Some(systemError)
        }
    }
}
