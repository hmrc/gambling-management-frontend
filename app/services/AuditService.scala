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

import models.audit.*
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.*
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditService @Inject() (
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext) {

  private val auditSource: String = "gambling-management-frontend"

  def sendEvent[A <: AuditEventModel](
    auditEvent: A
  )(implicit hc: HeaderCarrier, request: Request[?]): Future[AuditResult] = {
    val extendedDataEvent = ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditEvent.auditType,
      detail = auditEvent.detailJson,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags()
    )

    auditConnector.sendExtendedEvent(extendedDataEvent)
  }
}
