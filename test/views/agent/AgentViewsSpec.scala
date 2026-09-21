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

package views.agent

import base.SpecBase
import org.jsoup.Jsoup
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import viewmodels.agent.ClientListViewModel
import views.html.agent.*

class AgentViewsSpec extends SpecBase {

  private val app                          = applicationBuilder().build()
  private implicit val request: Request[?] = FakeRequest()
  private implicit val messages: Messages  = app.injector.instanceOf[MessagesApi].preferred(request)

  "RetrievingClientView" - {
    "renders the retrieving heading" in {
      val doc = Jsoup.parse(app.injector.instanceOf[RetrievingClientView].apply().body)
      doc.title             must include(messages("retrievingClient.title"))
      doc.select("h1").text must include(messages("retrievingClient.heading"))
    }
  }

  "FailedToRetrieveClientView" - {
    "renders the failure heading and paragraph" in {
      val doc = Jsoup.parse(app.injector.instanceOf[FailedToRetrieveClientView].apply().body)
      doc.select("h1").text must include(messages("failedToRetrieveClient.heading"))
      doc.select("p").text  must include(messages("failedToRetrieveClient.p1"))
    }
  }

  "NoAuthorisedClientsView" - {
    "renders the no-clients heading" in {
      val doc = Jsoup.parse(app.injector.instanceOf[NoAuthorisedClientsView].apply().body)
      doc.select("h1").text must include(messages("noAuthorisedClients.heading"))
    }
  }

  "AgentLostAccessView" - {
    "renders the lost-access heading" in {
      val doc = Jsoup.parse(app.injector.instanceOf[AgentLostAccessView].apply().body)
      doc.select("h1").text must include(messages("agentLostAccess.heading"))
    }
  }

  "AgentLandingView" - {
    "renders the client name, reg number and the registration-certificate link" in {
      val view = app.injector.instanceOf[AgentLandingView]
      val doc  = Jsoup.parse(view("Acme Casinos", "XMM00000000123").body)

      doc.select("h1").text                    must include("Acme Casinos")
      doc.text                                 must include("XMM00000000123")
      doc.select("a").attr("href")             must not be empty
      doc.select("a").eachAttr("href").toArray must contain(
        controllers.routes.ViewRegistrationCertificateController.onPageLoad().url
      )
    }
  }

  "ClientListSearchView" - {
    val view = app.injector.instanceOf[ClientListSearchView]

    "renders a row with a select link per client" in {
      val clients = Seq(
        ClientListViewModel("Acme Casinos", "RN1", "ref1"),
        ClientListViewModel("Bingo Ltd", "RN2", "ref2")
      )
      val doc     = Jsoup.parse(view(clients, "").body)

      doc.select("tbody tr").size mustBe 2
      doc.text                                 must include("Acme Casinos")
      doc.select("a").eachAttr("href").toArray must contain(
        controllers.agent.routes.AgentLandingController.onPageLoad("RN1").url
      )
    }

    "renders the no-results message when the list is empty" in {
      val doc = Jsoup.parse(view(Seq.empty, "abc").body)
      doc.text must include(messages("clientListSearch.noResults"))
    }
  }

  "SecurityCheckView" - {
    "renders the checking-authorisation heading" in {
      val doc = Jsoup.parse(app.injector.instanceOf[views.html.SecurityCheckView].apply().body)
      doc.select("h1").text must include(messages("securityCheck.heading"))
    }
  }
}
