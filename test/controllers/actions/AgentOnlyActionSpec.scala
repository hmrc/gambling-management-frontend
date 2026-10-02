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

import base.SpecBase
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AgentOnlyActionSpec extends SpecBase {

  class Harness(agentOnlyAction: AgentOnlyAction) {
    def onPageLoad: Action[AnyContent] = agentOnlyAction { request =>
      Results.Ok(s"${request.isAgent}|${request.agentReference.getOrElse("")}")
    }
  }

  val bodyParser: BodyParsers.Default = BodyParsers.Default(Helpers.stubPlayBodyParsers)

  private type Retrievals = Option[String] ~ Option[AffinityGroup] ~ Enrolments

  private def retrievalResult(
    affinityGroup: Option[AffinityGroup],
    enrolments: Enrolments,
    internalId: Option[String] = Some("internal-id")
  ): Retrievals =
    new ~(new ~(internalId, affinityGroup), enrolments)

  private def mockConnectorReturning(result: => Future[Retrievals]): AuthConnector = {
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[Retrievals])(using _: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(result)
    mockAuthConnector
  }

  private val agentEnrolment =
    Enrolments(Set(Enrolment("HMRC-MGD-AGNT", Seq(EnrolmentIdentifier("HMRCMGDAGENTREF", "1234567890")), "Activated")))

  private val orgEnrolment =
    Enrolments(Set(Enrolment("HMRC-MGD-ORG", Seq(EnrolmentIdentifier("HMRCMGDRN", "1234567890")), "Activated")))

  private val accessDenied = controllers.routes.AccessDeniedController.onPageLoad().url

  "AgentOnlyAction" - {

    "allow an agent with an active agent enrolment through" in {
      val connector = mockConnectorReturning(Future.successful(retrievalResult(Some(AffinityGroup.Agent), agentEnrolment)))
      val action    = new DefaultAgentOnlyAction(connector, testAppConfig, bodyParser)

      val result = new Harness(action).onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe OK
      contentAsString(result) mustBe "true|1234567890"
    }

    "deny an organisation (with org enrolment) and redirect to access denied" in {
      val connector = mockConnectorReturning(Future.successful(retrievalResult(Some(AffinityGroup.Organisation), orgEnrolment)))
      val action    = new DefaultAgentOnlyAction(connector, testAppConfig, bodyParser)

      val result = new Harness(action).onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(accessDenied)
    }

    "deny an organisation with no enrolment and redirect to access denied" in {
      val connector = mockConnectorReturning(Future.successful(retrievalResult(Some(AffinityGroup.Organisation), Enrolments(Set()))))
      val action    = new DefaultAgentOnlyAction(connector, testAppConfig, bodyParser)

      val result = new Harness(action).onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(accessDenied)
    }

    "deny an individual and redirect to access denied" in {
      val connector = mockConnectorReturning(Future.successful(retrievalResult(Some(AffinityGroup.Individual), Enrolments(Set()))))
      val action    = new DefaultAgentOnlyAction(connector, testAppConfig, bodyParser)

      val result = new Harness(action).onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(accessDenied)
    }

    "deny an agent with no active enrolment and redirect to access denied" in {
      val connector = mockConnectorReturning(Future.successful(retrievalResult(Some(AffinityGroup.Agent), Enrolments(Set()))))
      val action    = new DefaultAgentOnlyAction(connector, testAppConfig, bodyParser)

      val result = new Harness(action).onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(accessDenied)
    }

    "deny a request with no affinity group and redirect to access denied" in {
      val connector = mockConnectorReturning(Future.successful(retrievalResult(None, Enrolments(Set()))))
      val action    = new DefaultAgentOnlyAction(connector, testAppConfig, bodyParser)

      val result = new Harness(action).onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(accessDenied)
    }

    "redirect to login when there is no active session" in {
      val connector = mockConnectorReturning(Future.failed(new NoActiveSession("No session") {}))
      val action    = new DefaultAgentOnlyAction(connector, testAppConfig, bodyParser)

      val result = new Harness(action).onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value must startWith(testAppConfig.loginUrl)
    }
  }
}
