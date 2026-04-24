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

package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.{ReturnSummary, ReturnSummaryError}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class GamblingConnectorISpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  // ------------------------------------------
  // WireMock setup
  // ------------------------------------------

  private val wireMockServer = new WireMockServer(0)

  override def beforeAll(): Unit = {
    wireMockServer.start()
    configureFor("localhost", wireMockServer.port())
  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  // ------------------------------------------
  // Test App (AFTER WireMock starts)
  // ------------------------------------------

  private lazy val app =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.gambling.host" -> "localhost",
        "microservice.services.gambling.port" -> wireMockServer.port()
      )
      .build()

  private lazy val connector =
    app.injector.instanceOf[GamblingConnector]

  private val mgdRegNumber = "XWM00000001762"

  "GamblingConnector.getReturnSummary" should {

    "return Right(summary) when backend returns 200" in {

      val responseJson =
        Json.obj(
          "mgdRegNumber"   -> mgdRegNumber,
          "returnsDue"     -> 5,
          "returnsOverdue" -> 2
        )

      wireMockServer.stubFor(
        get(urlEqualTo(s"/gambling/return-summary/$mgdRegNumber"))
          .willReturn(okJson(responseJson.toString()))
      )

      connector.getReturnSummary(mgdRegNumber).map { result =>
        result.isRight mustBe true
      }
    }

    "return Left(NotFound) when backend returns 404" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/gambling/return-summary/$mgdRegNumber"))
          .willReturn(aResponse().withStatus(404))
      )

      connector.getReturnSummary(mgdRegNumber).map { result =>
        result mustBe Left(ReturnSummaryError.NotFound)
      }
    }

    "return Left(UnexpectedError) when backend returns 500" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/gambling/return-summary/$mgdRegNumber"))
          .willReturn(serverError())
      )

      connector.getReturnSummary(mgdRegNumber).map { result =>
        result mustBe Left(ReturnSummaryError.UnexpectedError)
      }
    }
  }
}
