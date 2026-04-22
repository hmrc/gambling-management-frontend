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

package base

import com.typesafe.config.ConfigFactory
import config.AppConfig
import controllers.actions.*
import models.UserAnswers
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import play.api.{Application, Configuration}
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import repositories.SessionRepository

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with MockFactory {

  implicit val actorSystem: ActorSystem = ActorSystem("unit-tests")
  implicit val mat: Materializer        = Materializer.createMaterializer(actorSystem)

  val userAnswersId: String = "id"

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[AuthorisedAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  protected val testAppConfig = new AppConfig(
    Configuration(ConfigFactory.parseString("""
                                              |host = "http://localhost:9000"
                                              | mongodb {
                                              |  timeToLiveInSeconds = 900
                                              | }
                                              | urls {
                                              |  login = "http://foo.com/login"
                                              |  loginContinue = "http://foo.com/bar"
                                              |  signOut = "http://foo.com/sign-out"
                                              |  homePageUrl = "http://foo.com/home"
                                              |  accessibilityStatementUrl = "http://foo.com/accessibility-statement"
                                              |  betaFeedbackUrl = "http://foo.com/beta-feedback"
                                              |  researchUrl = "http://foo.com/research"
                                              | }
                                              |  timeout-dialog {
                                              |   timeout   = 10
                                              |   countdown = 5
                                              | }
                                              | contact-frontend {
                                              |  host      = "http://localhost:9250"
                                              |  serviceId = "gambling-management-frontend"
                                              |}
                                              |microservice {
                                              |    services {
                                              |      auth {
                                              |        protocol = http
                                              |        host     = localhost
                                              |        port     = 8500
                                              |      }
                                              |
                                              |      feedback-frontend {
                                              |        protocol = http
                                              |        host     = localhost
                                              |        port     = 9514
                                              |      }
                                              |    }
                                              |}
                                              |features {
                                              |  welsh-translation: false
                                              |}
                                              |""".stripMargin))
  )

}
