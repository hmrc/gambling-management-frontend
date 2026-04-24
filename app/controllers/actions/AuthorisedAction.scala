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

@ImplementedBy(classOf[DefaultAuthorisedAction])
trait AuthorisedAction
    extends ActionBuilder[AuthorisedRequest, AnyContent]
    with ActionFunction[Request, AuthorisedRequest]

@Singleton
class DefaultAuthorisedAction @Inject() (
  override val authConnector: AuthConnector,
  config: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthorisedAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(Retrievals.affinityGroup.and(Retrievals.allEnrolments)) {
        case Some(affinityGroup @ AffinityGroup.Agent) ~ AuthorisedAction.HasActiveAgentEnrolment(
              mgdRegNum
            ) =>
          block(AuthorisedRequest(request, affinityGroup, mgdRegNum))
        case Some(AffinityGroup.Agent) ~ _ =>
          logger.warn(s"Agent auth failed: enrolment missing or not activated for ${request.path}")
          Future.failed(InsufficientEnrolments("Agent enrolment missing or not activated"))

        case Some(affinityGroup @ AffinityGroup.Organisation) ~ AuthorisedAction.HasActiveOrganisationEnrolment(
              mgdRegNum
            ) =>
          block(AuthorisedRequest(request, affinityGroup, mgdRegNum))
        case Some(AffinityGroup.Organisation) ~ _ =>
          logger.warn(s"Organisation auth failed: enrolment missing or not activated for ${request.path}")
          Future.failed(InsufficientEnrolments("Organisation enrolment missing or not activated"))

        case _ =>
          logger.warn(s"Auth failed: no affinity group found for ${request.path}")
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

object AuthorisedAction {
  val organisationEnrolmentKey   = "HMRC-MGD-ORG"
  val organisationIdentifierName = "HMRCMGDRN"
  val agentEnrolmentKey          = "HMRC-MGD-AGNT"
  val agentIdentifierName        = "HMRCMGDAGENTREF"

  def getEnrolmentIdentifier(
    enrolments: Enrolments,
    enrolmentKey: String,
    identifierName: String
  ): Option[String] =
    enrolments.getEnrolment(enrolmentKey).flatMap { enrolment =>
      Option
        .when(enrolment.isActivated)(
          enrolment.getIdentifier(identifierName).map(_.value)
        )
        .flatten
        .filter(_.nonEmpty)
    }

  object HasActiveAgentEnrolment {
    def unapply(enrolments: Enrolments): Option[String] =
      getEnrolmentIdentifier(enrolments, agentEnrolmentKey, agentIdentifierName)
  }

  object HasActiveOrganisationEnrolment {
    def unapply(enrolments: Enrolments): Option[String] =
      getEnrolmentIdentifier(enrolments, organisationEnrolmentKey, organisationIdentifierName)
  }
}
