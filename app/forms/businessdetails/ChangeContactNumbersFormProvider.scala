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

package forms.businessdetails

import forms.mappings.Validation.faxRegex
import forms.mappings.{Constants, Mappings}
import models.Enumerable
import models.agent.ContactNumbers
import play.api.data.Forms.{mapping, of, optional}
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}

import javax.inject.Inject

class ChangeContactNumbersFormProvider @Inject() extends Mappings {

  private val maxLengthChars = 20
  private val allowedChars   = "^[0-9 ]+$"

  def apply(): Form[ContactNumbers] =
    Form(
      mapping(
        "phoneNumber"  -> optional(
          text()
            .verifying(
              firstError(
                maxLength(maxLengthChars, "changeContactNumbers.error.length.phoneNumber"),
                regexp(allowedChars, "changeContactNumbers.error.invalid.phoneNumber")
              )
            )
        ),
        "mobileNumber" -> optional(
          text()
            .verifying(
              firstError(
                maxLength(maxLengthChars, "changeContactNumbers.error.length.mobileNumber"),
                regexp(allowedChars, "changeContactNumbers.error.invalid.mobileNumber")
              )
            )
        )
      )(ContactNumbers.apply)(contactNumbers => Some((contactNumbers.phoneNumber, contactNumbers.mobileNumber)))
        .verifying(
          "global_changeContactNumbers_error_required",
          contactNumbers => contactNumbers.phoneNumber.isDefined || contactNumbers.mobileNumber.isDefined
        )
    )
}
