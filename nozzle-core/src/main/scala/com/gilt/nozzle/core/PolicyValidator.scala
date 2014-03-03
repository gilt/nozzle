package com.gilt.nozzle.core

import spray.http.{HttpResponse, HttpRequest}
import scala.util.Try
import akka.actor.ActorRef
import spray.can.Http.ConnectionException

object PolicyValidator {
  type ValidatePolicy = (HttpRequest, DevInfo, TargetInfo) => Try[Unit]

  def defaultErrorHandler(e: Throwable, request: HttpRequest, d: Option[DevInfo], t: Option[TargetInfo]) = e match {
    case e: AuthorizationFailedException => HttpResponse(401)
    case e: AccessForbiddenException => HttpResponse(403)
    case e: NotFoundException => HttpResponse(404)
    case e: ConnectionException => HttpResponse(502)
    case _ => HttpResponse(500)
  }
}

case class ValidationMessage(request: HttpRequest, devInfo: DevInfo, targetInfo: TargetInfo, replyTo: ActorRef)