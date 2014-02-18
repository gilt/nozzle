package com.gilt.nozzle.core

import spray.http.HttpRequest
import scala.concurrent.Future

trait DevInfo {
  val id: String
  val roles: Iterable[Role]
  val name: Option[String]
  val email: Option[String]
}

case class DefaultDevInfo(val id: String, val roles: Iterable[Role], val name: Option[String], val email: Option[String])
    extends DevInfo

object DevInfo {
  def apply(s: String): DevInfo = DefaultDevInfo(s, Seq.empty[Role], None, None)
  type DevInfoExtractor = HttpRequest => Future[DevInfo]
  val Anonymous = DevInfo("anonymous")
}
