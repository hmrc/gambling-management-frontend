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

package forms

import forms.mappings.Mappings
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.{Form, FormError}
import models.Enumerable

object MappingsSpec {

  sealed trait Foo
  case object Bar extends Foo
  case object Baz extends Foo

  object Foo {

    val values: Set[Foo] = Set(Bar, Baz)

    implicit val fooEnumerable: Enumerable[Foo] =
      Enumerable(values.toSeq.map(v => v.toString -> v)*)
  }
}

class MappingsSpec extends AnyFreeSpec with Matchers with OptionValues with Mappings {

  import MappingsSpec.*

  "text" - {

    val testForm: Form[String] =
      Form(
        "value" -> text()
      )

    "must bind a valid string" in {
      val result = testForm.bind(Map("value" -> "foobar"))
      result.get mustEqual "foobar"
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string of non-breaking spaces only" in {
      val result = testForm.bind(Map("value" -> "\u00A0\u00A0"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return a custom error message" in {
      val form   = Form("value" -> text("custom.error"))
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "custom.error"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill("foobar")
      result.apply("value").value.value mustEqual "foobar"
    }
  }

  "optionalText" - {

    val testForm: Form[Option[String]] =
      Form(
        "value" -> optionalText()
      )

    "must bind and normalise a value" in {
      testForm.bind(Map("value" -> "  foo    bar ")).get mustEqual Some("foo bar")
    }

    "must bind None for an empty string" in {
      testForm.bind(Map("value" -> "")).get mustEqual None
    }

    "must bind None for a string of whitespace only, including non-breaking spaces" in {
      testForm.bind(Map("value" -> " \t\u00A0 ")).get mustEqual None
    }

    "must bind None for an empty map" in {
      testForm.bind(Map.empty[String, String]).get mustEqual None
    }

    "must unbind a value" in {
      testForm.fill(Some("foo")).apply("value").value.value mustEqual "foo"
    }

    "must unbind None as no value" in {
      testForm.fill(None).apply("value").value mustBe None
    }
  }

  "boolean" - {

    val testForm: Form[Boolean] =
      Form(
        "value" -> boolean()
      )

    "must bind true" in {
      val result = testForm.bind(Map("value" -> "true"))
      result.get mustEqual true
    }

    "must bind false" in {
      val result = testForm.bind(Map("value" -> "false"))
      result.get mustEqual false
    }

    "must not bind a non-boolean" in {
      val result = testForm.bind(Map("value" -> "not a boolean"))
      result.errors must contain(FormError("value", "error.boolean"))
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind" in {
      val result = testForm.fill(true)
      result.apply("value").value.value mustEqual "true"
    }
  }

  "int" - {

    val testForm: Form[Int] =
      Form(
        "value" -> int()
      )

    "must bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill(123)
      result.apply("value").value.value mustEqual "123"
    }
  }

  "enumerable" - {

    val testForm = Form(
      "value" -> enumerable[Foo]()
    )

    "must bind a valid option" in {
      val result = testForm.bind(Map("value" -> "Bar"))
      result.get mustEqual Bar
    }

    "must not bind an invalid option" in {
      val result = testForm.bind(Map("value" -> "Not Bar"))
      result.errors must contain(FormError("value", "error.invalid"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }
  }

  "currency" - {

    val testForm: Form[BigDecimal] =
      Form(
        "value" -> currency()
      )

    "must bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must bind a valid decimal with 1 decimal place" in {
      val result = testForm.bind(Map("value" -> "1.2"))
      result.get mustEqual 1.2
    }

    "must bind a valid decimal with 2 decimal places" in {
      val result = testForm.bind(Map("value" -> "1.23"))
      result.get mustEqual 1.23
    }

    "must bind a valid number with spaces, commas and `£` characters" in {
      val result = testForm.bind(Map("value" -> "£ 1,234 . 01"))
      result.get mustEqual 1234.01
    }

    "must bind negative numbers" in {
      val result = testForm.bind(Map("value" -> "-1"))
      result.get mustEqual -1
    }

    "must bind a negative decimal" in {
      val result = testForm.bind(Map("value" -> "-123.45"))
      result.get mustEqual -123.45
    }

    "must not bind values with non-numeric characters except commas, spaces and `£`s" in {
      val result = testForm.bind(Map("value" -> "abc"))
      result.errors must contain only FormError("value", "error.invalid")
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a number with more than 2 decimal places" in {
      val result = testForm.bind(Map("value" -> "1.234"))
      result.errors must contain only FormError("value", "error.invalid")
    }

    "must bind a value at the max boundary" in {
      val result = testForm.bind(Map("value" -> "999999999.99"))
      result.get mustEqual BigDecimal("999999999.99")
    }

    "must bind a value at the min boundary" in {
      val result = testForm.bind(Map("value" -> "-999999999.99"))
      result.get mustEqual BigDecimal("-999999999.99")
    }

    "must not bind a value exceeding the max" in {
      val result = testForm.bind(Map("value" -> "1000000000"))
      result.errors must contain only FormError("value", "error.range")
    }

    "must not bind a value below the min" in {
      val result = testForm.bind(Map("value" -> "-1000000000"))
      result.errors must contain only FormError("value", "error.range")
    }

    "must unbind a valid value" in {
      val result = testForm.fill(1)
      result.apply("value").value.value mustEqual "1"
    }
  }
}
