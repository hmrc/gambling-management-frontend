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

import config.AppConfig
import controllers.actions.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.GamblingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ViewRegistrationCertificateView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ViewRegistrationCertificateController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  view: ViewRegistrationCertificateView,
  gamblingService: GamblingService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen getData).async { implicit request =>

      val mgdRefNum = request.userId

      gamblingService
        .retrieveCertificate(mgdRefNum)
        .map { certificate =>

          val (displayName, displayLabelKey) =
            certificate.typeOfBusiness.map(_.trim.toLowerCase) match {

              case Some("sole proprietor") =>
                certificate.individualName.getOrElse("") ->
                  "viewRegistrationCertificate.label.soleProprietor"

              case Some(b) if b.startsWith("unincorporated body") =>
                certificate.businessName.getOrElse("") ->
                  "viewRegistrationCertificate.label.unincorporatedBody"

              case Some("corporate body") =>
                certificate.businessName.getOrElse("") ->
                  "viewRegistrationCertificate.label.corporateBody"

              case Some("partnership") =>
                certificate.businessName.getOrElse("") ->
                  "viewRegistrationCertificate.label.partnership"

              case Some("limited liability partnership") =>
                certificate.businessName.getOrElse("") ->
                  "viewRegistrationCertificate.label.limitedLiabilityPartnership"

              case _ =>
                certificate.businessName.getOrElse("") ->
                  "viewRegistrationCertificate.label.default"
            }

          val formattedAddress =
            Seq(
              certificate.busAddrLine1,
              certificate.busAddrLine2,
              certificate.busAddrLine3,
              certificate.busAddrLine4,
              certificate.busPostcode
            ).flatten.filter(_.nonEmpty).mkString("<br>")

          Ok(
            view(
              certificate,
              displayName,
              displayLabelKey,
              formattedAddress
            )
          )
        }
        .recover { case ex =>
          logger.error(
            s"[ViewRegistrationCertificateController] retrieveCertificate failed for mgdRefNum=$mgdRefNum",
            ex
          )
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }
}
