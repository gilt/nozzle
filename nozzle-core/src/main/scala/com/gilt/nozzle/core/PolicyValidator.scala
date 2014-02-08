package com.gilt.nozzle.core

import spray.http.{HttpResponse, HttpRequest}
import scala.util.Try
import akka.actor.ActorRef

object PolicyValidator {
  type ValidatePolicy = (HttpRequest, DevInfo, TargetInfo) => Try[Unit]

  def defaultErrorHandler(e: Throwable, request: HttpRequest, d: DevInfo, t: TargetInfo) = e match {
    case e: AuthorizationFailedException => HttpResponse(401)
    case e: AccessForbiddenException => HttpResponse(403)
    case _ => HttpResponse(501)
  }
}

case class ValidationMessage(request: HttpRequest, devInfo: DevInfo, targetInfo: TargetInfo, replyTo: ActorRef)