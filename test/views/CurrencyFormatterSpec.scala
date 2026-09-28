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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class CurrencyFormatterSpec extends AnyFreeSpec with Matchers {

  "currencyFormat" - {

    "must format whole numbers" in {
      CurrencyFormatter.currencyFormat(100) mustEqual "£100"
    }

    "must format decimal values" in {
      CurrencyFormatter.currencyFormat(BigDecimal("100.50")) mustEqual "£100.50"
    }

    "must use the absolute value for negatives" in {
      CurrencyFormatter.currencyFormat(BigDecimal("-100")) mustEqual "£100"
    }
  }
  "formattedAmountHtml" - {

    "must format negative values" in {

      val result = CurrencyFormatter.formattedAmountHtml(BigDecimal(-100))

      result must include("£100")
      result must include("nowrap")
    }

    "must format positive values" in {

      val result = CurrencyFormatter.formattedAmountHtml(BigDecimal(100))

      result must include("£100")
      result must include("nowrap")
    }
  }
}
