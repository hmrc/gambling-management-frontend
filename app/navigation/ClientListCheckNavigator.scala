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

package navigation

import models.agent.ClientListCheckReturnTarget
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

/** Builds the SecurityCheck in-progress spinner Call for each GroupB return target. When a GroupB page finds the
  * client-list retrieval still in progress, it redirects here; the spinner polls until the retrieval succeeds, then
  * returns to the original page.
  */
@Singleton
class ClientListCheckNavigator @Inject() () {

  def agentLanding(instanceId: String): Call =
    controllers.routes.SecurityCheckController.onClientListCheck(
      returnTo = ClientListCheckReturnTarget.AgentLanding.key,
      instanceId = Some(instanceId)
    )

  def clientList: Call =
    controllers.routes.SecurityCheckController.onClientListCheck(
      returnTo = ClientListCheckReturnTarget.ClientList.key,
      instanceId = None
    )

  def manageClientDetails: Call =
    controllers.routes.SecurityCheckController.onClientListCheck(
      returnTo = ClientListCheckReturnTarget.ManageClientDetails.key,
      instanceId = None
    )

  def changeClientReference(instanceId: String): Call =
    controllers.routes.SecurityCheckController.onClientListCheck(
      returnTo = ClientListCheckReturnTarget.ChangeClientReference.key,
      instanceId = Some(instanceId)
    )

  def removeClient(instanceId: String): Call =
    controllers.routes.SecurityCheckController.onClientListCheck(
      returnTo = ClientListCheckReturnTarget.RemoveClient.key,
      instanceId = Some(instanceId)
    )
}
