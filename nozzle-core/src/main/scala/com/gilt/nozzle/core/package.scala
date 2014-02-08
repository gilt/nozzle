package com.gilt.nozzle

import spray.http.{HttpResponse, HttpRequest}
import scala.concurrent.Future

package object core {
  type RequestEnricher = (HttpRequest, DevInfo, TargetInfo) => HttpRequest
  type ResponseEnricher = (HttpRequest, HttpResponse, DevInfo, TargetInfo) => HttpResponse
  type ValidationFailureHandler = (Throwable, HttpRequest, DevInfo, TargetInfo) => HttpResponse
  type ForwardRequest = (HttpRequest) => Future[HttpResponse]
}
