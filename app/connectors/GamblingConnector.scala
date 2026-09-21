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

import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import models.{GetClientListStatusResponse, MgdCertificate, ReturnSummary, ReturnSummaryError}
import models.agent.{AgentClient, AgentClientData, HasClientResponse, UpdateAgentClientRequest}
import models.requests.RemoveAgentClientRequest
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class GamblingConnector @Inject() (
  httpClient: HttpClientV2,
  servicesConfig: ServicesConfig
)(using ec: ExecutionContext):

  private val baseUrl =
    servicesConfig.baseUrl("gambling")

  private val agentBaseUrl = s"$baseUrl/gambling/agent"

  private val regime = "mgd"

  private given HttpReads[ReturnSummary] =
    HttpReads.Implicits.readFromJson[ReturnSummary]

  private given HttpReads[GetClientListStatusResponse] =
    HttpReads.Implicits.readFromJson[GetClientListStatusResponse]

  private given HttpReads[HasClientResponse] =
    HttpReads.Implicits.readFromJson[HasClientResponse]

  private given HttpReads[HttpResponse] = HttpReads.Implicits.readRaw

  def getReturnSummary(
    mgdRegNumber: String
  )(using hc: HeaderCarrier): Future[Either[ReturnSummaryError, ReturnSummary]] =

    val url = s"$baseUrl/gambling/return-summary/mgd/$mgdRegNumber"

    httpClient
      .get(url"$url")
      .execute[ReturnSummary]
      .map(Right(_))
      .recover {
        case e if e.getMessage.contains("404") =>
          Left(ReturnSummaryError.NotFound)
        case NonFatal(_)                       =>
          Left(ReturnSummaryError.UnexpectedError)
      }

  def getCertificate(mgdRegNumber: String)(implicit hc: HeaderCarrier): Future[MgdCertificate] =
    httpClient
      .get(url"$baseUrl/gambling/certificate/mgd/$mgdRegNumber")
      .execute[HttpResponse]
      .map { response =>
        response.status match {

          case OK =>
            response.json
              .validate[MgdCertificate]
              .fold(
                errors => throw new RuntimeException(s"Invalid JSON: $errors"),
                cert => cert
              )

          case status =>
            throw UpstreamErrorResponse(
              s"Unexpected status while fetching MGD certificate: $status",
              status
            )
        }
      }

  def startClientList(using HeaderCarrier): Future[GetClientListStatusResponse] =
    httpClient
      .post(url"$agentBaseUrl/client-list/$regime/retrieval/start")
      .execute[GetClientListStatusResponse]

  def getClientListStatus(using HeaderCarrier): Future[GetClientListStatusResponse] =
    httpClient
      .post(url"$agentBaseUrl/client-list/$regime/retrieval/status")
      .execute[GetClientListStatusResponse]

  def hasClient(regime: String, regNumber: String)(using HeaderCarrier): Future[HasClientResponse] =
    httpClient
      .get(url"$agentBaseUrl/has-client/$regime/$regNumber")
      .execute[HasClientResponse]

  def getAllClients(using HeaderCarrier): Future[List[AgentClient]] =
    httpClient
      .get(url"$agentBaseUrl/client-list/$regime")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK     =>
            (response.json \ "clients").as[Seq[JsValue]].toList.map { client =>
              AgentClient(
                regime = regime,
                regNumber = (client \ "regNumber").as[String],
                clientName = (client \ "clientName").asOpt[String],
                agentOwnRef = (client \ "agentOwnRef").asOpt[String]
              )
            }
          case status =>
            throw UpstreamErrorResponse(s"Unexpected status while fetching client list: $status", status)
        }
      }

  def saveAgentClient(userId: String, agentClientData: AgentClientData)(using HeaderCarrier): Future[Unit] =
    httpClient
      .post(url"$baseUrl/gambling/user-cache/agent-client/$userId")
      .withBody(Json.toJson(agentClientData))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => ()
          case _  => throw new HttpException(response.body, response.status)
        }
      }

  def updateClient(request: UpdateAgentClientRequest)(using HeaderCarrier): Future[Unit] =
    httpClient
      .post(url"$agentBaseUrl/update-client")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT => Future.unit
          case status     => Future.failed(UpstreamErrorResponse(response.body, status, status))
        }
      }

  def removeClient(request: RemoveAgentClientRequest)(using HeaderCarrier): Future[Unit] =
    httpClient
      .post(url"$agentBaseUrl/remove-client")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT => Future.unit
          case status     => Future.failed(UpstreamErrorResponse(response.body, status, status))
        }
      }
