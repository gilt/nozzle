package com.gilt.nozzle.core

import spray.http.{Uri, HttpRequest}
import scala.concurrent.Future


trait TargetInfo {
  val uri: Uri
  val roles: Iterable[Role]
}

case class DefaultTargetInfo(uri: Uri, roles: Iterable[Role]) extends TargetInfo

object TargetInfo {
  type TargetInfoExtractor = (HttpRequest) => Future[Option[TargetInfo]]
}
