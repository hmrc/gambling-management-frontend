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

class PaginationParamsSpec extends AnyFreeSpec with Matchers {

  "PaginationParams" - {

    "when totalRecords is 0" - {
      val params = PaginationParams(totalRecords = 0, pageSize = 10, pageNo = 1)

      "must have totalPages of 0" in { params.totalPages mustEqual 0 }
      "must have firstRecord of 0" in { params.firstRecord mustEqual 0 }
      "must have lastRecord of 0" in { params.lastRecord mustEqual 0 }
    }

    "when all records fit on one page" - {
      val params = PaginationParams(totalRecords = 5, pageSize = 10, pageNo = 1)

      "must have totalPages of 1" in { params.totalPages mustEqual 1 }
      "must have firstRecord of 1" in { params.firstRecord mustEqual 1 }
      "must have lastRecord equal to totalRecords" in { params.lastRecord mustEqual 5 }
    }

    "when records span multiple pages" - {
      val params = PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 1)

      "must compute totalPages by ceiling division" in { params.totalPages mustEqual 3 }
      "must have firstRecord of 1 on page 1" in { params.firstRecord mustEqual 1 }
      "must have lastRecord of 10 on page 1" in { params.lastRecord mustEqual 10 }
    }

    "on a middle page" - {
      val params = PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 2)

      "must have firstRecord of 11" in { params.firstRecord mustEqual 11 }
      "must have lastRecord of 20" in { params.lastRecord mustEqual 20 }
    }

    "on the last page with a partial page" - {
      val params = PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 3)

      "must have firstRecord of 21" in { params.firstRecord mustEqual 21 }
      "must cap lastRecord at totalRecords" in { params.lastRecord mustEqual 25 }
    }

    "when totalRecords divides evenly by pageSize" - {
      val params = PaginationParams(totalRecords = 20, pageSize = 10, pageNo = 2)

      "must have totalPages of 2" in { params.totalPages mustEqual 2 }
      "must have lastRecord of 20 on the final page" in { params.lastRecord mustEqual 20 }
    }

    "isOutOfRange" - {
      "must be false when totalRecords is 0" in {
        PaginationParams(totalRecords = 0, pageSize = 10, pageNo = 1).isOutOfRange mustBe false
      }
      "must be false when pageNo is within range" in {
        PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 2).isOutOfRange mustBe false
      }
      "must be false when pageNo equals totalPages" in {
        PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 3).isOutOfRange mustBe false
      }
      "must be true when pageNo exceeds totalPages" in {
        PaginationParams(totalRecords = 25, pageSize = 10, pageNo = 4).isOutOfRange mustBe true
      }
    }
  }
}
