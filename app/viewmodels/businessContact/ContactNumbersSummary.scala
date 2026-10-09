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

package viewmodels.businessContact

import controllers.routes
import models.{NormalMode, UserAnswers}
import pages.BusinessDetailsPage
import pages.businessdetails.ChangeFaxNumberPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow

object ContactNumbersSummary {

  def rows(answers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val answer = answers.get(BusinessDetailsPage) // TODO maybe get from new Page instead (not written yet) ?

    val phoneNumber = answer match {
      case Some(bd) => bd.phoneNumber.getOrElse(messages("changeBusinessContact.notProvided"))
      case None => messages("changeBusinessContact.notProvided")
    }

    val mobileNumber = answer match {
      case Some(bd) => bd.mobileNumber.getOrElse(messages("changeBusinessContact.notProvided"))
      case None => messages("changeBusinessContact.notProvided")
    }

    Seq(
      BusinessContactHelpers.textOrActionLinkRowDouble(
        keyMsg = "changeBusinessContact.contactNumbers",
        answer = Some(
          Seq(
            messages("changeBusinessContact.contactNumbers.phone"),
            phoneNumber,
            "",
            messages("changeBusinessContact.contactNumbers.mobile"),
            mobileNumber
          ).mkString("<br>")
        ),
        urlChange = controllers.businessdetails.routes.ChangeFaxNumberController // TODO Change Phone Numbers Controller
          .onPageLoad()
          .url,
        hiddenMsg = "changeBusinessContact.contactNumbers"
      )
    )
  }
}
