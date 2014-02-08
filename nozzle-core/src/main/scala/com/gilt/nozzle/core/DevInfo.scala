package com.gilt.nozzle.core

import spray.http.HttpRequest
import scala.concurrent.Future

case class DevInfo(id: String, roles: Iterable[String], name: Option[String], email: Option[String])

object DevInfo {
  def apply(s: String): DevInfo = DevInfo(s, Seq.empty[String], None, None)
  type DevInfoExtractor = HttpRequest => Future[DevInfo]
}