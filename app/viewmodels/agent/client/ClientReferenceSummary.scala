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

package viewmodels.agent.client

import models.UserAnswers
import pages.BusinessDetailsPage
import pages.businessdetails.ChangeEmailAddressPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.businessContact.BusinessContactHelpers

object ClientReferenceSummary {

  def rows(clientReference: String, regNumber: String)(implicit
    messages: Messages
  ): SummaryListRow = // Seq[SummaryListRow] =
//    Seq(
    BusinessContactHelpers.textOrActionWithRemoveLinkRow(
      keyMsg = "manageClientDetails.clientReference",
      answer = Some(clientReference),
      onlyShowChangeLink = clientReference.isEmpty,
      notProvidedMsg = "manageClientDetails.notProvided",
      urlChange = controllers.clientdetails.routes.ChangeClientReferenceController.onPageLoad(regNumber).url,
      urlRemove = "#",
      hiddenMsg = "manageClientDetails.clientReference"
    )
//    )
}
