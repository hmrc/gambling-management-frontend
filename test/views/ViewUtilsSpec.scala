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

package views

import base.SpecBase
import play.api.data.Form
import play.api.data.Forms.text
import play.api.i18n.Messages

class ViewUtilsSpec extends SpecBase {

  implicit val msgs: Messages = messages(applicationBuilder().build())

  private val titleKey                  = "removeEmailAddress.title"
  private val cleanForm: Form[String]   = Form("value" -> text())
  private val erroredForm: Form[String] = cleanForm.withError("value", "error.required")

  "ViewUtils.title" - {

    "must not add a leading space or prefix when the form has no errors" in {
      ViewUtils.title(cleanForm, titleKey) mustBe
        s"${msgs(titleKey)} - ${msgs("service.name")} - ${msgs("site.govuk")}"
    }

    "must prefix the title with 'Error:' when the form has errors" in {
      ViewUtils.title(erroredForm, titleKey) mustBe
        s"${msgs("error.title.prefix")} ${msgs(titleKey)} - ${msgs("service.name")} - ${msgs("site.govuk")}"
    }
  }

  "ViewUtils.errorPrefix" - {

    "must be empty when the form has no errors" in {
      ViewUtils.errorPrefix(cleanForm) mustBe ""
    }

    "must be the colon-suffixed prefix when the form has errors" in {
      ViewUtils.errorPrefix(erroredForm) mustBe s"${msgs("error.title.prefix")} "
    }
  }
}
