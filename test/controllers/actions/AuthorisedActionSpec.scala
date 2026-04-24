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

class AuthorisedActionSpec extends SpecBase {

  class Harness(authorisedAction: AuthorisedAction) {
    def onPageLoad: Action[AnyContent] = authorisedAction { request =>
      Results.Ok(request.mgdRegNum)
    }
  }

  val bodyParser: BodyParsers.Default = BodyParsers.Default(Helpers.stubPlayBodyParsers)

  "AuthorisedAction" - {
    "create AuthorisedRequest when user has an Organisation affinity group" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              Some(AffinityGroup.Organisation),
              Enrolments(
                Set(Enrolment("HMRC-MGD-ORG", Seq(EnrolmentIdentifier("HMRCMGDRN", "1234567890")), "Activated"))
              )
            )
          )
        )
      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe OK
      contentAsString(result) mustBe "1234567890"
    }

    "create AuthorisedRequest when user has an Agent affinity group" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              Some(AffinityGroup.Agent),
              Enrolments(
                Set(Enrolment("HMRC-MGD-AGNT", Seq(EnrolmentIdentifier("HMRCMGDAGENTREF", "1234567890")), "Activated"))
              )
            )
          )
        )

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe OK
      contentAsString(result) mustBe "1234567890"
    }

    "redirect to access denied page when user has no affinity group" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              None,
              Enrolments(Set())
            )
          )
        )

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.AccessDeniedController.onPageLoad().url
      )
    }

    "redirect to access denied page when an agent has no enrolment" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              Some(AffinityGroup.Agent),
              Enrolments(Set())
            )
          )
        )

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.AccessDeniedController.onPageLoad().url
      )
    }

    "redirect to access denied page when an organisation has no enrolment" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              Some(AffinityGroup.Organisation),
              Enrolments(Set())
            )
          )
        )

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.AccessDeniedController.onPageLoad().url
      )
    }

    "redirect to access denied page when user is agent but has enrolment for organisation" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              Some(AffinityGroup.Agent),
              Enrolments(
                Set(Enrolment("HMRC-CHAR-ORG", Seq(EnrolmentIdentifier("CHARID", "1234567890")), "Activated"))
              )
            )
          )
        )

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.AccessDeniedController.onPageLoad().url
      )
    }

    "redirect to access denied page when user is an organisation but has enrolment for agent" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              Some(AffinityGroup.Organisation),
              Enrolments(
                Set(Enrolment("HMRC-CHAR-AGENT", Seq(EnrolmentIdentifier("AGENTCHARID", "1234567890")), "Activated"))
              )
            )
          )
        )

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.AccessDeniedController.onPageLoad().url
      )
    }

    "redirect to access denied page when an Individual affinity group tries to access with incorrect enrolment" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              Some(AffinityGroup.Individual),
              Enrolments(
                Set(Enrolment("HMRC-CHAR-IND", Seq(EnrolmentIdentifier("INDCHARID", "1234567890")), "Activated"))
              )
            )
          )
        )

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.AccessDeniedController.onPageLoad().url
      )
    }

    "redirect to access denied page when an Individual affinity group tries to access with agent enrolment" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(
          Future.successful(
            `~`(
              Some(AffinityGroup.Individual),
              Enrolments(
                Set(Enrolment("HMRC-CHAR-AGENT", Seq(EnrolmentIdentifier("AGENTCHARID", "1234567890")), "Activated"))
              )
            )
          )
        )

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.AccessDeniedController.onPageLoad().url
      )
    }

    "redirect to login when user has no active session" in {
      val mockAuthConnector: AuthConnector = mock[AuthConnector]

      (mockAuthConnector
        .authorise(_: Predicate, _: Retrieval[Option[AffinityGroup] ~ Enrolments])(using
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(Future.failed(new NoActiveSession("No session") {}))

      val authorisedAction =
        new DefaultAuthorisedAction(mockAuthConnector, testAppConfig, bodyParser)

      val controller = new Harness(authorisedAction)
      val result     = controller.onPageLoad(FakeRequest("GET", "/test"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value must startWith(testAppConfig.loginUrl)
    }

  }
}
