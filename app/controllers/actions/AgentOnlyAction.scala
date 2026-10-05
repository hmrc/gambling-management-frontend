/*
 * Copyright 2025 HM Revenue & Customs
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
import config.AppConfig
import models.requests.AuthorisedRequest
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultAgentOnlyAction])
trait AgentOnlyAction
    extends ActionBuilder[AuthorisedRequest, AnyContent]
    with ActionFunction[Request, AuthorisedRequest]

@Singleton
class DefaultAgentOnlyAction @Inject() (
  override val authConnector: AuthConnector,
  config: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AgentOnlyAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(Retrievals.internalId and Retrievals.affinityGroup and Retrievals.allEnrolments) {
        case internalIdOpt ~ Some(affinityGroup @ AffinityGroup.Agent) ~ AuthorisedAction.HasActiveAgentEnrolment(
              agentReference
            ) =>
          block(
            AuthorisedRequest(
              request,
              affinityGroup,
              mgdRegNum = "",
              userId = internalIdOpt.getOrElse(""),
              isAgent = true,
              agentReference = Some(agentReference)
            )
          )
        case _ ~ Some(AffinityGroup.Agent) ~ _ =>
          logger.warn(s"Agent auth failed: enrolment missing or not activated for ${request.path}")
          Future.failed(InsufficientEnrolments("Agent enrolment missing or not activated"))

        case _ ~ Some(ag) ~ _ =>
          logger.warn(s"Access denied for $ag for ${request.path}")
          Future.successful(Redirect(controllers.routes.AccessDeniedController.onPageLoad()))

        case _ =>
          logger.warn(s"Auth failed: no agent affinity group found for ${request.path}")
          Future.failed(UnsupportedAffinityGroup("No affinity group found"))

      }
      .recover {
        case _: InsufficientEnrolments | _: UnsupportedAffinityGroup | _: InsufficientConfidenceLevel |
            _: UnsupportedCredentialRole | _: UnsupportedAuthProvider =>
          Redirect(controllers.routes.AccessDeniedController.onPageLoad())
        case ex: AuthorisationException =>
          logger.info(s"Unauthenticated access to ${request.path}: ${ex.getMessage}")
          Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      }
  }
}
