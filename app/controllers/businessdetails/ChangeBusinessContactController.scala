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

package controllers.businessdetails

import controllers.actions.*
import models.Mode
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.all.stringToKey
import viewmodels.govuk.summarylist.{SummaryListRowViewModel, SummaryListViewModel, ValueViewModel}
import views.html.businessdetails.ChangeBusinessContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeBusinessContactController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  authorise: AuthorisedAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeBusinessContactView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authorise andThen getData andThen requireData) { implicit request =>

      val summaryList =
        SummaryListViewModel(
          rows = Seq(
            SummaryListRowViewModel(
              key = "Email",
              value = ValueViewModel(Text("test@example.com"))
            ),
            SummaryListRowViewModel(
              key = "Telephone",
              value = ValueViewModel(Text("020 1234 5678"))
            )
          )
        )
      Ok(view(summaryList, mode))
    }

  def onSubmit(): Action[AnyContent] =
    (authorise andThen getData andThen requireData).async { implicit request =>
      // TODO: Submit to CHRIS

      Future.successful(
        Redirect(
          controllers.routes.IndexController.onPageLoad()
        )
      )
    }
}
