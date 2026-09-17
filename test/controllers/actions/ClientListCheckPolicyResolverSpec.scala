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

package controllers.actions

import models.agent.ClientListCheckPolicy
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.RequestHeader
import play.api.routing.{HandlerDef, Router}
import play.api.test.FakeRequest

class ClientListCheckPolicyResolverSpec extends AnyFreeSpec with Matchers {

  private val resolver = new ClientListCheckPolicyResolver()

  private def requestFor(controller: String, method: String, httpMethod: String = "GET"): RequestHeader = {
    val handlerDef = HandlerDef(
      classLoader = getClass.getClassLoader,
      routerPackage = "router",
      controller = controller,
      method = method,
      parameterTypes = Seq.empty,
      verb = httpMethod,
      path = "/x"
    )
    FakeRequest(httpMethod, "/x").addAttr(Router.Attrs.HandlerDef, handlerDef)
  }

  "ClientListCheckPolicyResolver.resolve" - {

    "returns Exempt for non-GET requests" in {
      resolver.resolve(
        requestFor("controllers.ViewRegistrationCertificateController", "onPageLoad", "POST")
      ) mustBe ClientListCheckPolicy.Exempt
    }

    "returns Exempt for an exempt controller" in {
      resolver.resolve(
        requestFor("controllers.agent.RetrievingClientController", "start")
      ) mustBe ClientListCheckPolicy.Exempt
      resolver.resolve(
        requestFor("controllers.IndexController", "onPageLoad")
      ) mustBe ClientListCheckPolicy.Exempt
    }

    "returns GroupB for a GroupB route" in {
      resolver.resolve(
        requestFor("controllers.agent.ClientListSearchController", "onPageLoad")
      ) mustBe ClientListCheckPolicy.GroupB
      resolver.resolve(
        requestFor("controllers.clientdetails.ManageClientDetailsController", "onPageLoad")
      ) mustBe ClientListCheckPolicy.GroupB
    }

    "returns GroupA for an explicit GroupA route" in {
      resolver.resolve(
        requestFor("controllers.ViewRegistrationCertificateController", "onPageLoad")
      ) mustBe ClientListCheckPolicy.GroupA
    }

    "returns GroupA for any other onPageLoad route" in {
      resolver.resolve(
        requestFor("controllers.some.OtherController", "onPageLoad")
      ) mustBe ClientListCheckPolicy.GroupA
    }

    "returns Exempt for a non-onPageLoad method with no matching route" in {
      resolver.resolve(
        requestFor("controllers.some.OtherController", "someAction")
      ) mustBe ClientListCheckPolicy.Exempt
    }
  }

  "ClientListCheckPolicyResolver.shouldRunCentralHasClient" - {

    "is false for a central-has-client-exempt route" in {
      resolver.shouldRunCentralHasClient(
        requestFor("controllers.clientdetails.ClientRemovedController", "onPageLoad")
      ) mustBe false
    }

    "is true for a normal route" in {
      resolver.shouldRunCentralHasClient(
        requestFor("controllers.ViewRegistrationCertificateController", "onPageLoad")
      ) mustBe true
    }
  }
}
