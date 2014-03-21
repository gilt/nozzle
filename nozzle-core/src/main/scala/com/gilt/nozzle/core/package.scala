package com.gilt.nozzle

import spray.http.{HttpResponsePart, HttpResponse, HttpRequest}
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

package object core {
  type RequestTransformer = (HttpRequest, DevInfo, TargetInfo) => HttpRequest
  type ResponseTransformer = (HttpRequest, HttpResponsePart, DevInfo, TargetInfo) => HttpResponsePart
  type ValidationFailureHandler = (Throwable, HttpRequest, Option[DevInfo], Option[TargetInfo]) => HttpResponse
  type ForwardRequest = (HttpRequest) => Future[HttpResponsePart]
  type DevKeyExtractor = (HttpRequest) => Option[DevKey]
  type Role = String
  type DevKey = String

  object defaults {
    lazy val config = ConfigFactory.load.getConfig("nozzle")
  }
}
