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
import models.{CheckMode, Mode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.all.stringToKey
import viewmodels.govuk.summarylist.{SummaryListRowViewModel, SummaryListViewModel, ValueViewModel}
import views.html.businessdetails.ChangeBusinessContactView
import viewmodels.govuk.summarylist.*

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

      val phoneNumber  = "0191 202 2500"
      val mobileNumber = "07890 123 456"

      val summaryList = SummaryListViewModel(
        rows = Seq(
          SummaryListRowViewModel(
            key = KeyViewModel(Text("Contact numbers")),
            value = ValueViewModel(
              HtmlContent(
                Html(
                  s"""
                     |Phone number:<br>
                     |$phoneNumber
                     |<br><br>
                     |Mobile number:<br>
                     |$mobileNumber
                      """.stripMargin
                )
              )
            ).withCssClass("govuk-!-width-one-half"),
            actions = Seq(
              ActionItemViewModel(
                Text("Change"),
                controllers.routes.IndexController // ChangeContactNumbersController
                  .onPageLoad()
                  .url
              )
            )
          ),
          SummaryListRowViewModel(
            key = KeyViewModel(Text("Fax number")),
            value = ValueViewModel(Text("01881123456")),
            actions = Seq(
              ActionItemViewModel(
                Text("Change"),
                controllers.businessdetails.routes.ChangeFaxNumberController
                  .onPageLoad()
                  .url
              ),
              ActionItemViewModel(
                Text("Remove"),
                controllers.businessdetails.routes.RemoveFaxNumberController
                  .onPageLoad()
                  .url
              )
            )
          ),
          SummaryListRowViewModel(
            key = KeyViewModel(Text("Email address")),
            value = ValueViewModel(Text("someone@someone.com")),
            actions = Seq(
              ActionItemViewModel(
                Text("Change"),
                controllers.businessdetails.routes.ChangeEmailAddressController
                  .onPageLoad()
                  .url
              ),
              ActionItemViewModel(
                Text("Remove"),
                controllers.businessdetails.routes.ChangeEmailAddressController // RemoveEmailAddressController
                  .onPageLoad()
                  .url
              )
            )
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
