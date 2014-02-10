package com.gilt.nozzle

import spray.http.{HttpResponse, HttpRequest}
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

package object core {
  type RequestEnricher = (HttpRequest, DevInfo, TargetInfo) => HttpRequest
  type ResponseEnricher = (HttpRequest, HttpResponse, DevInfo, TargetInfo) => HttpResponse
  type ValidationFailureHandler = (Throwable, HttpRequest, DevInfo, TargetInfo) => HttpResponse
  type ForwardRequest = (HttpRequest) => Future[HttpResponse]
  type DevKeyExtractor = (HttpRequest) => Option[DevKey]
  type Role = String
  type DevKey = String

  object defaults {
    lazy val config = ConfigFactory.load(Thread.currentThread().getContextClassLoader).getConfig("nozzle")
  }
}
