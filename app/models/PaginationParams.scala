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

case class PaginationParams(totalRecords: Int, pageSize: Int, pageNo: Int) {
  val totalPages: Int = if (totalRecords == 0) 0 else Math.ceil(totalRecords.toDouble / pageSize).toInt
  val firstRecord: Int = if (totalRecords == 0) 0 else (pageNo - 1) * pageSize + 1
  val lastRecord: Int = if (totalRecords == 0) 0 else Math.min(pageNo * pageSize, totalRecords)
  val isOutOfRange: Boolean = totalPages > 0 && pageNo > totalPages
}
