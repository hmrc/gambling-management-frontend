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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.*
import queries.{Gettable, Settable}

import java.time.Instant
import scala.util.{Failure, Success}

class UserAnswersSpec extends AnyFreeSpec with Matchers with OptionValues {

  case object TestPage extends Gettable[String] with Settable[String] {
    override val path: JsPath = JsPath \ "test"

    override def cleanup(value: Option[String], userAnswers: UserAnswers) =
      Success(userAnswers)
  }

  "UserAnswers" - {

    "get" - {

      "must return value when present" in {
        val json = Json.obj("test" -> "value")
        val ua   = UserAnswers("id", json)

        ua.get(TestPage).value mustEqual "value"
      }

      "must return None when value is missing" in {
        val ua = UserAnswers("id")

        ua.get(TestPage) mustEqual None
      }
    }

    "set" - {

      "must set a value" in {
        val ua = UserAnswers("id")

        val result = ua.set(TestPage, "value")

        result mustBe a[Success[_]]

        val updated = result.get

        (updated.data \ "test").as[String] mustEqual "value"
      }

      "must overwrite existing value" in {
        val ua = UserAnswers("id", Json.obj("test" -> "old"))

        val result = ua.set(TestPage, "new")

        result.get.data mustEqual Json.obj("test" -> "new")
      }

      "must return failure when write fails" in {

        case object BadPage extends Settable[String] {
          override val path: JsPath = JsPath

          override def cleanup(value: Option[String], userAnswers: UserAnswers) =
            Success(userAnswers)
        }

        val ua = UserAnswers("id")

        val result = ua.set(BadPage, "value")

        result mustBe a[Failure[_]]
      }
    }

    "remove" - {

      "must remove existing value" in {
        val ua = UserAnswers("id", Json.obj("test" -> "value"))

        val result = ua.remove(TestPage)

        result.get.data mustEqual Json.obj()
      }

      "must do nothing if value does not exist" in {
        val ua = UserAnswers("id")

        val result = ua.remove(TestPage)

        result.get.data mustEqual Json.obj()
      }
    }

    "format" - {

      "must serialise and deserialise correctly" in {
        import java.time.temporal.ChronoUnit

        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        val ua = UserAnswers(
          id = "id",
          data = Json.obj("test" -> "value"),
          lastUpdated = now
        )

        val json = Json.toJson(ua)

        val parsed = json.as[UserAnswers]

        parsed.id mustEqual "id"
        parsed.data mustEqual Json.obj("test" -> "value")
        parsed.lastUpdated mustEqual now
      }
    }
  }
}
