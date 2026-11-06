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

package viewmodels.agent

import base.SpecBase
import controllers.routes
import models.agent.AgentClientUtil
import pages.BusinessDetailsPage
import pages.businessdetails.{ChangeEmailAddressPage, RemoveEmailAddressPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}

class ClientListViewModelSpec extends SpecBase {

  "ClientListViewModel" - {

    "should convert List[AgentClient] to Seq[ClientListViewModel]" in {
      val result = ClientListViewModel.fromAgentClients(AgentClientUtil.clientsList)
      result mustBe AgentClientUtil.clientsListAsViewModel
    }

    "must filter a Seq[ClientListViewModel] when provided name is ALL CAPS" in {
      val result = ClientListViewModel.filterByName("CON", AgentClientUtil.clientsListAsViewModel)
      result mustBe List(
        ClientListViewModel(
          regNumber = "XCM00000001003",
          clientName = "Betelgeuse Construction 3",
          clientReference = "UP374"
        ),
        ClientListViewModel(
          regNumber = "XEM00000001004",
          clientName = "Heritage Contractors 4",
          clientReference = "WY080"
        )
      )
    }

    "must filter a Seq[ClientListViewModel] when provided name has SPACES before and after" in {
      val result = ClientListViewModel.filterByName("  CON  ", AgentClientUtil.clientsListAsViewModel)
      result mustBe List(
        ClientListViewModel(
          regNumber = "XCM00000001003",
          clientName = "Betelgeuse Construction 3",
          clientReference = "UP374"
        ),
        ClientListViewModel(
          regNumber = "XEM00000001004",
          clientName = "Heritage Contractors 4",
          clientReference = "WY080"
        )
      )
    }

    "must return an empty Seq[ClientListViewModel] when provided name is NOT FOUND" in {
      val result = ClientListViewModel.filterByName("NOT_FOUND", AgentClientUtil.clientsListAsViewModel)
      result mustBe List.empty
    }

    "must return the original Seq[ClientListViewModel] when provided nameQuery is EMPTY" in {
      val result = ClientListViewModel.filterByName("", AgentClientUtil.clientsListAsViewModel)
      result mustBe AgentClientUtil.clientsListAsViewModel
    }
  }
}
