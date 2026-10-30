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

package services

import play.api.i18n.Messages
import viewmodels.agent.ClientListViewModel
import viewmodels.govuk.PaginationFluency.*

import javax.inject.{Inject, Singleton}

case class PaginationConfig(
                             recordsPerPage: Int = 10,
                             maxRecords: Int = 99,
                             maxVisiblePages: Int = 5
                           )

case class ClientListPaginationResult(
                                       paginatedData: Seq[ClientListViewModel],
                                       paginationViewModel: PaginationViewModel,
                                       totalRecords: Int,
                                       currentPage: Int,
                                       totalPages: Int
                                     )

@Singleton
class PaginationService @Inject() {

  private val config = PaginationConfig()

  def paginateClientList(
                          allClients: Seq[ClientListViewModel],
                          currentPage: Int = 1,
                          baseUrl: String,
                          sortBy: Option[String] = None,
                          sortOrder: Option[String] = None
                        )(implicit messages: Messages): ClientListPaginationResult = {

    val totalRecords     = allClients.length
    val totalPages       = calculateTotalPages(totalRecords)
    val validCurrentPage = validateCurrentPage(currentPage, totalPages)

    val (startIndex, endIndex) = calculatePageIndices(validCurrentPage, totalRecords)

    val paginatedData = allClients.slice(startIndex, endIndex)

    val paginationViewModel = createPaginationViewModel(
      currentPage = validCurrentPage,
      totalPages = totalPages,
      baseUrl = baseUrl,
      sortBy = sortBy,
      sortOrder = sortOrder
    )

    ClientListPaginationResult(
      paginatedData = paginatedData,
      paginationViewModel = paginationViewModel,
      totalRecords = totalRecords,
      currentPage = validCurrentPage,
      totalPages = totalPages
    )
  }

  private def calculateTotalPages(totalRecords: Int): Int =
    Math.ceil(totalRecords.toDouble / config.recordsPerPage).toInt

  private def validateCurrentPage(currentPage: Int, totalPages: Int): Int =
    Math.max(1, Math.min(currentPage, totalPages))

  private def calculatePageIndices(currentPage: Int, totalRecords: Int): (Int, Int) = {
    val startIndex = (currentPage - 1) * config.recordsPerPage
    val endIndex   = Math.min(startIndex + config.recordsPerPage, totalRecords)
    (startIndex, endIndex)
  }

  private def createPaginationViewModel(
                                         currentPage: Int,
                                         totalPages: Int,
                                         baseUrl: String,
                                         sortBy: Option[String],
                                         sortOrder: Option[String]
                                       )(implicit messages: Messages): PaginationViewModel =
    if (totalPages <= 1) {
      PaginationViewModel()
    } else {
      val items    = generatePageItems(currentPage, totalPages, baseUrl, sortBy, sortOrder)
      val previous = createPreviousLink(currentPage, baseUrl, sortBy, sortOrder)
      val next     = createNextLink(currentPage, totalPages, baseUrl, sortBy, sortOrder)

      PaginationViewModel(
        items = items,
        previous = previous,
        next = next
      )
    }


  private def buildUrlWithParams(
                                  baseUrl: String,
                                  page: Int,
                                  sortBy: Option[String],
                                  sortOrder: Option[String]
                                ): String = {

    val params = scala.collection.mutable.ListBuffer[String]()
    params += s"page=$page"
    sortBy.foreach(sb => params += s"sortBy=$sb")
    sortOrder.foreach(so => params += s"sortOrder=$so")

    val separator = if (baseUrl.contains("?")) "&" else "?"
    s"$baseUrl$separator${params.mkString("&")}"
  }

  private def createPreviousLink(
                                  currentPage: Int,
                                  baseUrl: String,
                                  sortBy: Option[String],
                                  sortOrder: Option[String]
                                )(implicit messages: Messages): Option[PaginationLinkViewModel] =
    if (currentPage > 1) {
      Some(
        PaginationLinkViewModel(buildUrlWithParams(baseUrl, currentPage - 1, sortBy, sortOrder))
          .withText("site.pagination.previous")
          .withAriaLabel(messages("site.pagination.goToPage", currentPage - 1))
      )
    } else {
      None
    }

  private def createNextLink(
                              currentPage: Int,
                              totalPages: Int,
                              baseUrl: String,
                              sortBy: Option[String],
                              sortOrder: Option[String]
                            )(implicit messages: Messages): Option[PaginationLinkViewModel] =
    if (currentPage < totalPages) {
      Some(
        PaginationLinkViewModel(buildUrlWithParams(baseUrl, currentPage + 1, sortBy, sortOrder))
          .withText("site.pagination.next")
          .withAriaLabel(messages("site.pagination.goToPage", currentPage + 1))
      )
    } else {
      None
    }

  private def createPageItem(
                              page: Int,
                              currentPage: Int,
                              baseUrl: String,
                              sortBy: Option[String],
                              sortOrder: Option[String]
                            ): PaginationItemViewModel =
    PaginationItemViewModel(
      number = page.toString,
      href = buildUrlWithParams(
        baseUrl,
        page,
        sortBy,
        sortOrder
      )
    )
      .withVisuallyHiddenText("site.pagination.goToPage")
      .withCurrent(page == currentPage)

  private def generatePageItems(
                                 currentPage: Int,
                                 totalPages: Int,
                                 baseUrl: String,
                                 sortBy: Option[String],
                                 sortOrder: Option[String]
                               ): Seq[PaginationItemViewModel] = {
    val pageRange = calculatePageRange(currentPage, totalPages)
    val items     = scala.collection.mutable.ListBuffer[PaginationItemViewModel]()

    if (pageRange.head > 1) {
      items += createPageItem(
        page = 1,
        currentPage = currentPage,
        baseUrl = baseUrl,
        sortBy = sortBy,
        sortOrder = sortOrder
      )

      if (pageRange.head > 2) {
        items += PaginationItemViewModel.ellipsis()
      }
    }

    pageRange.foreach { page =>
      items += createPageItem(
        page = page,
        currentPage = currentPage,
        baseUrl = baseUrl,
        sortBy = sortBy,
        sortOrder = sortOrder
      )
    }

    if (pageRange.last < totalPages) {
      if (pageRange.last < totalPages - 1) {
        items += PaginationItemViewModel.ellipsis()
      }

      items += createPageItem(
        page = totalPages,
        currentPage = currentPage,
        baseUrl = baseUrl,
        sortBy = sortBy,
        sortOrder = sortOrder
      )
    }

    items.toSeq
  }

  private def calculatePageRange(currentPage: Int, totalPages: Int): Range = {
    val halfVisible = config.maxVisiblePages / 2
    val startPage   = Math.max(1, currentPage - halfVisible)
    val endPage     = Math.min(totalPages, startPage + config.maxVisiblePages - 1)

    val adjustedStartPage = if (endPage - startPage < config.maxVisiblePages - 1) {
      Math.max(1, endPage - config.maxVisiblePages + 1)
    } else startPage

    adjustedStartPage to endPage
  }
}
