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

package controllers

import controllers.actions.*
import models.BusinessDetails
import pages.BusinessDetailsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CheckBusinessDetailsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckBusinessDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckBusinessDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authorise andThen getData andThen requireData) { implicit request =>
    Ok(view(businessDetails(request.userAnswers)))
  }

  private def businessDetails(userAnswers: models.UserAnswers): BusinessDetails =
    userAnswers
      .get(BusinessDetailsPage)
      .getOrElse(
        BusinessDetails(
          businessName = Some("Agent1"),
          addressLine1 = Some("123 Business road"),
          addressLine2 = Some("Business"),
          addressLine3 = Some("London"),
          phoneNumber = Some("0191 202 2500"),
          mobileNumber = Some("07890 123 456"),
          faxNumber = Some("0800 202 2500"),
          emailAddress = Some("sarah.phillips@example.com")
        )
      )
}
