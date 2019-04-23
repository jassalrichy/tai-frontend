/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.tai.connectors

import com.google.inject.{Inject, Singleton}
import play.Logger
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsValue, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.tai.config.WSHttp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class HttpHandler @Inject()(val http: WSHttp) {

  def getFromApi(url: String)(implicit hc: HeaderCarrier): Future[JsValue] = {

    implicit val reads = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
        response.status match {
          case Status.OK => Try(response) match {
            case Success(data) => data
            case Failure(exception) => throw new RuntimeException(s"Unable to parse response: $exception")
          }
          case Status.NOT_FOUND => {
            Logger.warn(s"HttpHandler - No DATA Found")
            throw new NotFoundException(response.body)
          }
          case Status.INTERNAL_SERVER_ERROR => {
            Logger.warn(s"HttpHandler - Internal Server error")
            throw new InternalServerException(response.body)
          }
          case Status.BAD_REQUEST => {
            Logger.warn(s"HttpHandler - Bad request exception")
            throw new BadRequestException(response.body)
          }
          case Status.LOCKED => {
            Logger.warn(s"HttpHandler - Locked received")
            throw new LockedException(response.body)
          }
          case _ => {
            Logger.warn(s"HttpHandler - Server error received")
            throw new HttpException(response.body, response.status)
          }
        }
      }
    }

    http.GET[HttpResponse](url) map(_.json)

  }

  def putToApi[I](url: String, data: I)(implicit hc: HeaderCarrier, rds: HttpReads[I], writes: Writes[I]): Future[HttpResponse] = {

    implicit val reads = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
        response.status match {
          case OK => response
          case NOT_FOUND =>
            Logger.warn(s"HttpHandler - No data can be found")
            throw new NotFoundException(response.body)

          case INTERNAL_SERVER_ERROR =>
            Logger.warn(s"HttpHandler - Internal Server Error received")
            throw new InternalServerException(response.body)

          case BAD_REQUEST =>
            Logger.warn(s"HttpHandler - Bad Request received")
            throw new BadRequestException(response.body)

          case _ =>
            Logger.warn(s"HttpHandler - Server error received")
            throw new HttpException(response.body, response.status)
        }
      }
    }

    http.PUT[I, HttpResponse](url, data)
  }

  def postToApi[I](url: String, data: I)(implicit hc: HeaderCarrier, writes: Writes[I]): Future[HttpResponse] = {

    implicit val rawHttpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
        response.status match {
          case OK | CREATED => response
          case _ =>
            Logger.warn(s"HttpHandler - Error received with status: ${response.status} and body: ${response.body}")
            throw new HttpException(response.body, response.status)
        }
      }
    }

    http.POST[I, HttpResponse](url, data)
  }

  def deleteFromApi(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    implicit val rawHttpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
        response.status match {
          case OK | NO_CONTENT | ACCEPTED => response
          case _ =>
            Logger.warn(s"HttpHandler - Error received with status: ${response.status} and body: ${response.body}")
            throw new HttpException(response.body, response.status)
        }
      }


    }
    http.DELETE[HttpResponse](url)
  }
}
