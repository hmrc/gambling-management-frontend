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

package services

import models.audit.AuthFailureAuditEventModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  private given HeaderCarrier = HeaderCarrier()
  private given Request[?]    = FakeRequest()

  "sendEvent forwards an extended data event to the audit connector" in {
    val connector = mock[AuditConnector]
    when(connector.sendExtendedEvent(any[ExtendedDataEvent])(using any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(AuditResult.Success))

    val service = new AuditService(connector)
    service.sendEvent(AuthFailureAuditEventModel()).futureValue mustBe AuditResult.Success
    verify(connector).sendExtendedEvent(any[ExtendedDataEvent])(using any[HeaderCarrier], any[ExecutionContext])
  }
}
