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

package models

import play.api.i18n.Messages
import play.api.libs.json.*
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait BusinessType {
  def code: Int
}
object BusinessType extends Enumerable.Implicits {

  case object Soleproprietor extends WithName("soleproprietor") with BusinessType { val code = 1 }
  case object Corporatebody extends WithName("corporatebody") with BusinessType { val code = 2 }
  case object Unincorporatedbody extends WithName("unincorporatedbody") with BusinessType { val code = 3 }
  case object Partnership extends WithName("partnership") with BusinessType { val code = 4 }
  case object LimitedLiabilityPartnership extends WithName("llp") with BusinessType { val code = 5 }

  val values: Seq[BusinessType] = Seq(
    Soleproprietor,
    Corporatebody,
    Unincorporatedbody,
    Partnership,
    LimitedLiabilityPartnership
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"businessType.${value.toString}")),
      value = Some(value.toString),
      id = Some(s"value_$index")
    )
  }

  def fromCode(code: Int): Option[BusinessType] =
    values.find(_.code == code)

  implicit val enumerable: Enumerable[BusinessType] =
    Enumerable(values.map(v => v.toString -> v)*)

  implicit val reads: Reads[BusinessType] =
    Reads { json =>
      json.validate[Int].flatMap { code =>
        fromCode(code)
          .map(JsSuccess(_))
          .getOrElse(JsError("Invalid business type"))
      }
    }

  implicit val writes: Writes[BusinessType] =
    Writes(bt => JsNumber(bt.code))

  implicit val format: Format[BusinessType] =
    Format(reads, writes)
}
